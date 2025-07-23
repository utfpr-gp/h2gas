package com.h2gas.services;

import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.dto.OrderRequestDTO;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.OrderItem;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.OrderRequestMapper;
import com.h2gas.model.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductItemService productItemService;

    @Autowired
    private OrderRequestMapper orderRequestMapper;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressService addressService;

    public Order createOrder(OrderRequestDTO orderRequestDTO) {
        Order order = orderRequestMapper.toEntity(orderRequestDTO);
        Supplier supplier = supplierService.findEntityById(orderRequestDTO.getSupplierId());
        User user = userService.findEntityByIdUser(orderRequestDTO.getCustomerId());
        List<OrderItem> listOrderItems = new ArrayList<>();

        order.setCustomer(user);
        order.setSupplier(supplier);
        order.setOrderDate(LocalDateTime.now());
        order.setDeliveryAddress(addressService.findEntityByUserId(user.getId()));

        for (OrderItemRequestDTO item: orderRequestDTO.getItems()) {
            OrderItem orderItem = orderItemService.createOrderItem(item, order);
            listOrderItems.add(orderItem);
        }

        order.setTotalPrice(calculateTotalPrice(listOrderItems));
        order.setItems(listOrderItems);

        orderRepository.save(order);

        return order;
    }

    private BigDecimal calculateTotalPrice(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
