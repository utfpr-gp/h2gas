package com.h2gas.services;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.dto.OrderRequestDTO;
import com.h2gas.model.dto.OrderResponseDTO;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.OrderItem;
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
        order.setDeliveryAddress(addressService.findEntityByUserId(user.getId()));
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

    public Page<OrderResponseDTO> findByCustomerId(String id, Pageable pageable) {
        return orderRepository.findByCustomerId(id, pageable)
                .map(orderMapper::toResponseDto);
    }

    public Page<OrderResponseDTO> findBySupplierId(Long id, Pageable pageable) {
        return orderRepository.findBySupplierIdOrderByCreatedAtDesc(id, pageable)
                .map(orderMapper::toResponseDto);
    }

    public Page<OrderResponseDTO> findBySupplierIdAndStatus(Long id, OrderStatusEnum status, Pageable pageable) {
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

    public Page<OrderResponseDTO> findBySupplierIdAndStatusNotDeliveryAndCanceled(Long id, Pageable pageable, List<OrderStatusEnum> excludeStatuses) {
//        List<OrderStatusEnum> excludedStatuses = List.of(OrderStatusEnum.DELIVERED, OrderStatusEnum.CANCELED);
        return orderRepository.findBySupplierIdAndStatusNotInOrderByCreatedAtDesc(id, excludeStatuses, pageable)
                .map(orderMapper::toResponseDto);
    }
}
