package com.h2gas.services;

import com.h2gas.model.Coordinates;
import com.h2gas.model.DeliveryFeeCalculator;
import com.h2gas.model.DeliveryQuote;

import com.h2gas.model.dto.OrderHistorySummaryDTO;
import com.h2gas.model.dto.OrderHistoryFilter;
import com.h2gas.events.OrderStatusChangedEvent;
import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.DeliveryHistoryDTO;
import com.h2gas.model.dto.DeliveryHistoryItemDTO;
import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.dto.OrderPreviewDTO;
import com.h2gas.model.dto.OrderPreviewItemDTO;
import com.h2gas.model.dto.OrderPreviewRequestDTO;
import com.h2gas.model.dto.OrderRequestDTO;
import com.h2gas.model.dto.OrderResponseDTO;
import com.h2gas.model.dto.OrderReviewDTO;
import com.h2gas.model.dto.ReorderItemDTO;
import com.h2gas.model.dto.ReorderPreviewDTO;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.CategoryProduct;
import com.h2gas.model.entity.Deliverer;
import com.h2gas.model.entity.DeliveryRefusal;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.OrderItem;
import com.h2gas.model.entity.ProductItem;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.OrderMapper;
import com.h2gas.model.repository.DeliveryRefusalRepository;
import com.h2gas.model.repository.OrderRepository;
import com.h2gas.model.repository.ReviewRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private final DeliveryRefusalRepository deliveryRefusalRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private final ApplicationEventPublisher eventPublisher;

    public OrderService(DeliveryRefusalRepository deliveryRefusalRepository, ApplicationEventPublisher eventPublisher) {
        this.deliveryRefusalRepository = deliveryRefusalRepository;
        this.eventPublisher = eventPublisher;
    }

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

        // HU17: frete por km calculado no fechamento e gravado (RN01); retirada não cobra frete
        DeliveryQuote quote = deliveryQuote(order);
        order.setDeliveryFee(quote.fee());
        order.setDeliveryDistanceKm(quote.distanceKm());
        order.setTotalPrice(calculateTotalPrice(listOrderItems).add(quote.fee()));
        order.setItems(listOrderItems);

        orderRepository.save(order);

        return orderMapper.toResponseDto(order);
    }

    public OrderPreviewDTO previewOrder(OrderPreviewRequestDTO request) {
        DeliveryModeEnum deliveryMode = resolveDeliveryMode(request.getDeliveryMode());
        List<OrderPreviewItemDTO> items = new ArrayList<>();

        for (OrderItemRequestDTO item : request.getItems()) {
            ProductItem productItem = productItemService.findEntityById(item.getProductItemRef());
            BigDecimal unitPrice = OrderItemService.unitPriceFor(productItem);
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
                unavailable.add(new ReorderItemDTO(null, null, null, null, null, null, 0, null, "PRODUTO_REMOVIDO"));
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
                product.getPrice() != null ? product.getPrice().doubleValue() : null,
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

    // HU23: contagem por status da loja para o filtro do painel; status sem pedido = 0
    @Transactional(readOnly = true)
    public com.h2gas.model.dto.OrderStatusCountsDTO countMyOrdersByStatus(String supplierId) {
        java.util.Map<OrderStatusEnum, Long> counts = new java.util.EnumMap<>(OrderStatusEnum.class);
        for (OrderStatusEnum status : OrderStatusEnum.values()) counts.put(status, 0L);
        BigDecimal receivable = BigDecimal.ZERO;
        for (var row : orderRepository.countBySupplierGroupedByStatus(supplierId)) {
            if (row.getStatus() == null) continue;
            counts.put(row.getStatus(), row.getTotal());
            // HU28: a receber = pedidos em andamento (o pagamento é na entrega)
            if (row.getStatus() != OrderStatusEnum.DELIVERED && row.getStatus() != OrderStatusEnum.CANCELED && row.getAmount() != null) {
                receivable = receivable.add(row.getAmount());
            }
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        long active = total - counts.get(OrderStatusEnum.DELIVERED) - counts.get(OrderStatusEnum.CANCELED);
        return new com.h2gas.model.dto.OrderStatusCountsDTO(counts, active, total, receivable);
    }

    private DeliveryQuote deliveryQuote(Order order) {
        if (order.getDeliveryMode() == DeliveryModeEnum.PICKUP) {
            return new DeliveryQuote(true, null, BigDecimal.ZERO);
        }
        Supplier supplier = order.getSupplier();
        Coordinates store = supplier.getAddress() == null ? null : supplier.getAddress().coordinates();
        DeliveryQuote quote = DeliveryFeeCalculator.quote(supplier.deliveryTerms(), store, order.getDeliveryAddress().coordinates());
        if (!quote.available()) {
            throw new BusinessRuleException("Esta loja não entrega no endereço escolhido.");
        }
        return quote;
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

    // F12: o pedido só aparece para quem participa dele (cliente, loja, entregador) ou para o admin
    public OrderResponseDTO findByIdOrder(String id, User actor) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com esse id: " + id));
        if (!participates(order, actor)) {
            throw new ForbiddenResourceException("Você não tem acesso a este pedido.");
        }
        return orderMapper.toResponseDto(order);
    }

    private boolean participates(Order order, User actor) {
        String actorId = actor.getId();
        return actor.getUserType() == UserTypeEnum.ADMIN
                || (order.getCustomer() != null && actorId.equals(order.getCustomer().getId()))
                || isStoreOf(order, actor)
                || (order.getDeliverer() != null && order.getDeliverer().getUser() != null
                    && actorId.equals(order.getDeliverer().getUser().getId()));
    }

    private boolean isStoreOf(Order order, User actor) {
        return order.getSupplier() != null && actor.getId().equals(order.getSupplier().getId());
    }

    public Page<OrderResponseDTO> findByCustomerId(String customerId, String status, Pageable pageable) {
        Page<Order> page;

        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("TODOS")) {
            OrderStatusEnum statusEnum = OrderStatusEnum.valueOf(status.toUpperCase());
            page = orderRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(customerId, statusEnum, pageable);
        } else {
            page = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        }

        Page<OrderResponseDTO> dtos = page.map(orderMapper::toResponseDto);
        attachReviews(dtos.getContent());
        return dtos;
    }

    // HU24: a avaliação de cada pedido da página, sem uma consulta por pedido
    private void attachReviews(List<OrderResponseDTO> orders) {
        if (orders.isEmpty()) return;
        Map<String, OrderReviewDTO> byOrder = reviewRepository
                .findByOrderIdIn(orders.stream().map(OrderResponseDTO::getId).toList()).stream()
                .collect(java.util.stream.Collectors.toMap(r -> r.getOrder().getId(), ReviewService::toDto));
        orders.forEach(o -> o.setReview(byOrder.get(o.getId())));
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
        Order saved = orderRepository.save(order);
        publishStatusChange(saved);
        return saved;
    }

    // HU07: despacho = atribuir (ou trocar) o entregador; o pedido segue em preparo até o entregador iniciar
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
        return orderMapper.toResponseDto(orderRepository.save(order));
    }

    // HU07: o entregador toca "Iniciar Entrega"; é aqui que o pedido sai em rota
    @Transactional
    public OrderResponseDTO startDelivery(String orderId, Long delivererId) {
        Order order = findOrderOfDeliverer(orderId, delivererId);
        if (order.getStatus() != OrderStatusEnum.IN_PREPARATION) {
            throw new BusinessRuleException("Só é possível iniciar a entrega de pedidos em preparação.");
        }
        // HU07 (3ª revisão): uma entrega em rota por vez; atribuir a quem está em rota continua permitido
        if (orderRepository.existsByDelivererIdAndStatus(delivererId, OrderStatusEnum.OUT_FOR_DELIVERY)) {
            throw new BusinessRuleException("Finalize a entrega em rota antes de iniciar outra.");
        }
        order.setStatus(OrderStatusEnum.OUT_FOR_DELIVERY);
        order.setDispatchedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        publishStatusChange(saved);
        return orderMapper.toResponseDto(saved);
    }

    // HU07: a rota é só do entregador autenticado (o id vem do token, nunca de parâmetro livre);
    // em rota primeiro (despacho mais antigo), depois os pendentes de início (pedido mais antigo)
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> findActiveByDeliverer(Long delivererId) {
        List<Order> emRota = orderRepository.findByDelivererIdAndStatusOrderByDispatchedAtAsc(delivererId, OrderStatusEnum.OUT_FOR_DELIVERY);
        List<Order> pendentes = orderRepository.findByDelivererIdAndStatusOrderByCreatedAtAsc(delivererId, OrderStatusEnum.IN_PREPARATION);
        return Stream.concat(emRota.stream(), pendentes.stream())
                .map(orderMapper::toResponseDto)
                .toList();
    }

    // HU07 (Figura 17c): entregues e recusadas do entregador logado, da mais recente para a mais antiga
    @Transactional(readOnly = true)
    public DeliveryHistoryDTO findDeliveryHistory(Long delivererId) {
        List<Order> delivered = orderRepository.findByDelivererIdAndStatusOrderByDeliveredAtDesc(delivererId, OrderStatusEnum.DELIVERED);
        List<DeliveryRefusal> refusals = deliveryRefusalRepository.findByDelivererIdOrderByCreatedAtDesc(delivererId);

        List<DeliveryHistoryItemDTO> items = Stream.concat(
                        delivered.stream().map(order -> historyItem(order, "DELIVERED", order.getDeliveredAt(), order.getTotalPrice())),
                        refusals.stream().map(refusal -> historyItem(refusal.getOrder(), "REFUSED", refusal.getCreatedAt(), null)))
                .sorted(Comparator.comparing(DeliveryHistoryItemDTO::getOccurredAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int deliveredToday = (int) delivered.stream()
                .filter(order -> order.getDeliveredAt() != null && order.getDeliveredAt().toLocalDate().equals(LocalDate.now()))
                .count();
        return new DeliveryHistoryDTO(deliveredToday, items);
    }

    private DeliveryHistoryItemDTO historyItem(Order order, String kind, LocalDateTime occurredAt, BigDecimal totalPrice) {
        Address address = order.getDeliveryAddress();
        String summary = order.getItems() == null ? "" : order.getItems().stream()
                .map(item -> item.getQuantity() + "x " + item.getProduct().getBrand() + " " + item.getProduct().getCategoryProduct().getName())
                .collect(Collectors.joining(", "));
        return new DeliveryHistoryItemDTO(order.getId(), kind, occurredAt,
                address == null ? null : address.getStreetName(),
                address == null ? null : address.getNumber(),
                address == null ? null : address.getNeighborhood(),
                address == null || address.getCity() == null ? null : address.getCity().getName(),
                summary, order.getPaymentMethod(), totalPrice);
    }

    // HU07: o entregador recusa; o pedido volta ao fornecedor (não é cancelado ao cliente, estoque intacto)
    @Transactional
    public OrderResponseDTO cancelDelivery(String orderId, Long delivererId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Informe o motivo para cancelar a entrega.");
        }
        Order order = findOrderOfDeliverer(orderId, delivererId);
        if (order.getStatus() != OrderStatusEnum.IN_PREPARATION && order.getStatus() != OrderStatusEnum.OUT_FOR_DELIVERY) {
            throw new BusinessRuleException("Só é possível cancelar a entrega de pedidos atribuídos e ainda não entregues.");
        }
        DeliveryRefusal refusal = deliveryRefusalRepository.save(
                new DeliveryRefusal(null, order, order.getDeliverer(), reason.trim(), LocalDateTime.now()));
        if (order.getRefusals() == null) {
            order.setRefusals(new ArrayList<>());
        }
        order.getRefusals().add(0, refusal);
        order.setDeliverer(null);
        order.setDispatchedAt(null);
        order.setStatus(OrderStatusEnum.IN_PREPARATION);
        return orderMapper.toResponseDto(orderRepository.save(order));
    }

    // HU07: confirmação da entrega com um toque pelo entregador
    @Transactional
    public OrderResponseDTO completeDelivery(String orderId, Long delivererId) {
        Order order = findOrderOfDeliverer(orderId, delivererId);
        if (order.getStatus() != OrderStatusEnum.OUT_FOR_DELIVERY) {
            throw new BusinessRuleException("Só é possível concluir pedidos que estão em rota.");
        }
        order.setStatus(OrderStatusEnum.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        publishStatusChange(saved);
        return orderMapper.toResponseDto(saved);
    }

    // HU08: avisa a tela do cliente (via SSE, depois do commit). A recusa do entregador
    // (cancelDelivery) e o despacho (assignDeliverer) não passam por aqui, por decisão do script.
    private void publishStatusChange(Order order) {
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                order.getId(), order.getCustomer().getId(), order.getStatus().getStatus()));
    }

    private Order findOrderOfDeliverer(String orderId, Long delivererId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido com " + orderId + " não encontrado"));
        if (order.getDeliverer() == null || !order.getDeliverer().getId().equals(delivererId)) {
            throw new ForbiddenResourceException("Pedido não está atribuído a este entregador.");
        }
        return order;
    }

    // F12: só a loja do pedido (ou o admin) muda o status
    @Transactional
    public String updateOrderStatus(String id, String newStatus, User actor) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido com " + id + " não encontrado"));
        if (actor.getUserType() != UserTypeEnum.ADMIN && !isStoreOf(order, actor)) {
            throw new ForbiddenResourceException("Pedido não pertence ao fornecedor logado.");
        }
        return updateOrderStatus(id, newStatus);
    }

    @Transactional
    public String updateOrderStatus(String id, String newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido com " + id + " não encontrado"));

        // pedido de entrega só sai da loja por despacho com entregador (HU07); retirada segue por aqui
        if (OrderStatusEnum.OUT_FOR_DELIVERY.name().equals(newStatus) && order.getDeliveryMode() == DeliveryModeEnum.DELIVERY) {
            throw new BusinessRuleException("Use o despacho com entregador.");
        }

        order.setStatus(OrderStatusEnum.valueOf(newStatus));
        publishStatusChange(orderRepository.save(order));
        return order.getStatus().getStatus();
    }

    public Page<OrderResponseDTO> findBySupplierIdAndStatusNotDeliveryAndCanceled(String id, Pageable pageable, List<OrderStatusEnum> excludeStatuses) {
//        List<OrderStatusEnum> excludedStatuses = List.of(OrderStatusEnum.DELIVERED, OrderStatusEnum.CANCELED);
        return orderRepository.findBySupplierIdAndStatusNotInOrderByCreatedAtDesc(id, excludeStatuses, pageable)
                .map(orderMapper::toResponseDto);
    }

    // F7 fatia 6: histórico de pedidos finalizados do fornecedor do token
    public Page<OrderResponseDTO> findMyHistory(String supplierId, OrderHistoryFilter f, Pageable pageable) {
        return orderRepository.findHistory(supplierId, f.statuses(), f.from(), f.toExclusive(), f.payment(), f.mode(), f.q(), pageable)
                .map(orderMapper::toResponseDto);
    }

    // RN01: contagens e receita calculadas no banco; a receita é só dos entregues
    public OrderHistorySummaryDTO summarizeMyHistory(String supplierId, OrderHistoryFilter f) {
        long delivered = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        long canceled = 0;
        if (f.statuses().contains(OrderStatusEnum.DELIVERED)) {
            Object[] row = orderRepository.summarizeHistory(supplierId, List.of(OrderStatusEnum.DELIVERED),
                    f.from(), f.toExclusive(), f.payment(), f.mode(), f.q()).get(0);
            delivered = ((Number) row[0]).longValue();
            revenue = new BigDecimal(row[1].toString());
        }
        if (f.statuses().contains(OrderStatusEnum.CANCELED)) {
            canceled = ((Number) orderRepository.summarizeHistory(supplierId, List.of(OrderStatusEnum.CANCELED),
                    f.from(), f.toExclusive(), f.payment(), f.mode(), f.q()).get(0)[0]).longValue();
        }
        BigDecimal averageTicket = delivered == 0 ? null : revenue.divide(BigDecimal.valueOf(delivered), 2, java.math.RoundingMode.HALF_UP);
        return new OrderHistorySummaryDTO(delivered, canceled, revenue.setScale(2, java.math.RoundingMode.HALF_UP), averageTicket);
    }
}
