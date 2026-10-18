package com.h2gas.services;

import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.dto.OrderPreviewDTO;
import com.h2gas.model.dto.OrderPreviewItemDTO;
import com.h2gas.model.dto.OrderPreviewRequestDTO;
import com.h2gas.model.dto.OrderRequestDTO;
import com.h2gas.model.dto.OrderResponseDTO;
import com.h2gas.model.dto.ReorderItemDTO;
import com.h2gas.model.dto.ReorderPreviewDTO;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.CategoryProduct;
import com.h2gas.model.entity.Deliverer;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.OrderItem;
import com.h2gas.model.entity.ProductItem;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.OrderMapper;
import com.h2gas.model.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductItemService productItemService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private DelivererService delivererService;

    // RN02: pedido, itens e decremento de estoque entram ou saem juntos
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO orderRequestDTO) {
        Order order = orderMapper.toRequestEntity(orderRequestDTO);
        order.setItems(null);
        Supplier supplier = supplierService.findEntityById(orderRequestDTO.getSupplierRef());
        User user = userService.findEntityByIdUser(orderRequestDTO.getCustomerUuid());
        List<OrderItem> listOrderItems = new ArrayList<>();

        order.setCustomer(user);
        order.setSupplier(supplier);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatusEnum.PENDING);
        order.setDeliveryMode(resolveDeliveryMode(orderRequestDTO.getDeliveryMode()));
        // HU05: retirada na loja não tem endereço de entrega
        order.setDeliveryAddress(order.getDeliveryMode() == DeliveryModeEnum.PICKUP
                ? null
                : resolveDeliveryAddress(orderRequestDTO.getAddressId(), user));
        orderRepository.save(order);

        for (OrderItemRequestDTO item: orderRequestDTO.getItems()) {
            OrderItem orderItem = orderItemService.createOrderItem(item, order);
            listOrderItems.add(orderItem);
        }

        order.setTotalPrice(calculateTotalPrice(listOrderItems));
        order.setItems(listOrderItems);

        orderRepository.save(order);

        return orderMapper.toResponseDto(order);
    }

    public OrderPreviewDTO previewOrder(OrderPreviewRequestDTO request) {
        DeliveryModeEnum deliveryMode = resolveDeliveryMode(request.getDeliveryMode());
        List<OrderPreviewItemDTO> items = new ArrayList<>();

        for (OrderItemRequestDTO item : request.getItems()) {
            ProductItem productItem = productItemService.findEntityById(item.getProductItemRef());
            BigDecimal unitPrice = OrderItemService.unitPriceFor(productItem, deliveryMode);
            BigDecimal totalPrice = OrderItemService.calculateTotalPrice(item.getQuantity(), unitPrice);
            items.add(new OrderPreviewItemDTO(item.getProductItemRef(), item.getQuantity(), unitPrice, totalPrice));
        }

        BigDecimal subtotal = items.stream()
                .map(OrderPreviewItemDTO::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderPreviewDTO(items, subtotal);
    }

    // HU05: só leitura — usa dados e preços atuais do produto, nunca o unitPrice histórico (RN01)
    @Transactional(readOnly = true)
    public ReorderPreviewDTO buildReorderPreview(String orderId, User customer) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com esse id: " + orderId));
        if (order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenResourceException("Pedido não pertence ao cliente logado.");
        }

        List<ReorderItemDTO> available = new ArrayList<>();
        List<ReorderItemDTO> unavailable = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            ProductItem product = item.getProduct();
            if (product == null) {
                unavailable.add(new ReorderItemDTO(null, null, null, null, null, null, 0, null, null, "PRODUTO_REMOVIDO"));
            } else if (product.getStockQuantity() == null || product.getStockQuantity() <= 0) {
                unavailable.add(toReorderItem(product, 0, "SEM_ESTOQUE"));
            } else {
                available.add(toReorderItem(product, Math.min(item.getQuantity(), product.getStockQuantity()), null));
            }
        }

        return new ReorderPreviewDTO(available, unavailable);
    }

    private ReorderItemDTO toReorderItem(ProductItem product, int quantity, String unavailableReason) {
        CategoryProduct category = product.getCategoryProduct();
        return new ReorderItemDTO(
                product.getId(),
                product.getBrand(),
                category != null ? category.getName() : null,
                category != null && category.getProductType() != null ? category.getProductType().getName() : null,
                product.getImageUrl(),
                product.getSupplier() != null ? product.getSupplier().getId() : null,
                quantity,
                product.getPriceDelivery() != null ? product.getPriceDelivery().doubleValue() : null,
                product.getPricePickup() != null ? product.getPricePickup().doubleValue() : null,
                unavailableReason
        );
    }

    private DeliveryModeEnum resolveDeliveryMode(String deliveryMode) {
        if (deliveryMode == null || deliveryMode.isBlank()) {
            return DeliveryModeEnum.DELIVERY;
        }
        try {
            return DeliveryModeEnum.valueOf(deliveryMode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Modalidade de entrega inválida: " + deliveryMode);
        }
    }

    private Address resolveDeliveryAddress(Long addressId, User customer) {
        if (addressId == null) {
            throw new BusinessRuleException("Informe o endereço de entrega.");
        }
        Address address = addressService.findByIdEntity(addressId);
        if (address.getUser() == null || !address.getUser().getId().equals(customer.getId())) {
            throw new BusinessRuleException("Endereço não pertence ao cliente informado.");
        }
        return address;
    }

    private BigDecimal calculateTotalPrice(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    public Page<OrderResponseDTO> findAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(orderMapper::toResponseDto);
    }

    public OrderResponseDTO findByIdOrder(String id) {
        Optional<Order> orderOptional = orderRepository.findById(id);
        return orderOptional.map(orderMapper::toResponseDto).orElseThrow(()
                -> new ResourceNotFoundException("Pedido não encontrado com esse id: " + id));
    }

    public Page<OrderResponseDTO> findByCustomerId(String customerId, String status, Pageable pageable) {
        Page<Order> page;

        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("TODOS")) {
            OrderStatusEnum statusEnum = OrderStatusEnum.valueOf(status.toUpperCase());
            page = orderRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(customerId, statusEnum, pageable);
        } else {
            page = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        }

        return page.map(orderMapper::toResponseDto);
    }


    public Page<OrderResponseDTO> findBySupplierId(String id, Pageable pageable) {
        return orderRepository.findBySupplierIdOrderByCreatedAtDesc(id, pageable)
                .map(orderMapper::toResponseDto);
    }

    public Page<OrderResponseDTO> findBySupplierIdAndStatus(String id, OrderStatusEnum status, Pageable pageable) {
        return orderRepository.findBySupplierIdAndStatusOrderByCreatedAtDesc(id, status, pageable)
                .map(orderMapper::toResponseDto);
    }

    public Page<OrderResponseDTO> findByStatus(OrderStatusEnum status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable)
                .map(orderMapper::toResponseDto);
    }

    // HU06: cancelamento pelo fornecedor; o "estorno" é a devolução do estoque (não há gateway de pagamento)
    @Transactional
    public Order cancelOrder(String orderId, String reason, User actor) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Justificativa é obrigatória para cancelar um pedido.");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido com " + orderId + " não encontrado"));
        boolean isAdmin = actor.getUserType() == UserTypeEnum.ADMIN;
        if (!isAdmin && (order.getSupplier() == null || !order.getSupplier().getId().equals(actor.getId()))) {
            throw new ForbiddenResourceException("Pedido não pertence ao fornecedor logado.");
        }
        if (order.getStatus() == OrderStatusEnum.DELIVERED || order.getStatus() == OrderStatusEnum.CANCELED) {
            throw new BusinessRuleException("Pedido " + order.getStatus().getStatus().toLowerCase() + " não pode ser cancelado.");
        }

        for (OrderItem item : order.getItems()) {
            ProductItem product = item.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productItemService.save(product);
            }
        }
        order.setStatus(OrderStatusEnum.CANCELED);
        order.setCancellationReason(reason.trim());
        return orderRepository.save(order);
    }

    // HU07: despacho = atribuir entregador; leva o pedido de entrega a OUT_FOR_DELIVERY
    @Transactional
    public OrderResponseDTO assignDeliverer(String orderId, Long delivererId, User actor) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido com " + orderId + " não encontrado"));
        boolean isAdmin = actor.getUserType() == UserTypeEnum.ADMIN;
        if (!isAdmin && (order.getSupplier() == null || !order.getSupplier().getId().equals(actor.getId()))) {
            throw new ForbiddenResourceException("Pedido não pertence ao fornecedor logado.");
        }
        if (order.getStatus() != OrderStatusEnum.IN_PREPARATION) {
            throw new BusinessRuleException("Só é possível atribuir entregador a pedidos em preparação.");
        }
        if (order.getDeliveryMode() != DeliveryModeEnum.DELIVERY) {
            throw new BusinessRuleException("Pedido de retirada não tem entregador.");
        }

        Deliverer deliverer = delivererService.findEntityById(delivererId);
        if (!deliverer.getSupplier().getId().equals(order.getSupplier().getId())) {
            throw new BusinessRuleException("Entregador não pertence ao fornecedor do pedido.");
        }
        if (!deliverer.isActive()) {
            throw new BusinessRuleException("Entregador inativo.");
        }

        order.setDeliverer(deliverer);
        order.setStatus(OrderStatusEnum.OUT_FOR_DELIVERY);
        order.setDispatchedAt(LocalDateTime.now());
        return orderMapper.toResponseDto(orderRepository.save(order));
    }

    public String updateOrderStatus(String id, String newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido com " + id + " não encontrado"));

        // pedido de entrega só sai da loja por despacho com entregador (HU07); retirada segue por aqui
        if (OrderStatusEnum.OUT_FOR_DELIVERY.name().equals(newStatus) && order.getDeliveryMode() == DeliveryModeEnum.DELIVERY) {
            throw new BusinessRuleException("Use o despacho com entregador.");
        }

        order.setStatus(OrderStatusEnum.valueOf(newStatus));
        orderRepository.save(order);
        return order.getStatus().getStatus();
    }

    public Page<OrderResponseDTO> findBySupplierIdAndStatusNotDeliveryAndCanceled(String id, Pageable pageable, List<OrderStatusEnum> excludeStatuses) {
//        List<OrderStatusEnum> excludedStatuses = List.of(OrderStatusEnum.DELIVERED, OrderStatusEnum.CANCELED);
        return orderRepository.findBySupplierIdAndStatusNotInOrderByCreatedAtDesc(id, excludeStatuses, pageable)
                .map(orderMapper::toResponseDto);
    }
}
