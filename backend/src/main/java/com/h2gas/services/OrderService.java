package com.h2gas.services;

import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.dto.OrderPreviewDTO;
import com.h2gas.model.dto.OrderPreviewItemDTO;
import com.h2gas.model.dto.OrderPreviewRequestDTO;
import com.h2gas.model.dto.OrderRequestDTO;
import com.h2gas.model.dto.OrderResponseDTO;
import com.h2gas.model.entity.Address;
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
        order.setDeliveryAddress(resolveDeliveryAddress(orderRequestDTO.getAddressId(), user));
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

    public String updateOrderStatus(String id, String newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido com " + id + " não encontrado"));

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
