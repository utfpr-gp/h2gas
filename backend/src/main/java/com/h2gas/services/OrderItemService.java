package com.h2gas.services;

import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.OrderItem;
import com.h2gas.model.entity.ProductItem;
import com.h2gas.model.mapper.OrderItemMapper;
import com.h2gas.model.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderItemService {
    @Autowired
    private OrderItemRepository repository;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductItemService productItemService;

    public OrderItem createOrderItem(OrderItemRequestDTO orderItemRequestDTO, Order order) {
        ProductItem productItem = productItemService.findEntityById(orderItemRequestDTO.getProductItemId());
        OrderItem orderItem = orderItemMapper.toRequestEntity(orderItemRequestDTO);

        BigDecimal totalPrice = calculateTotalPrice(orderItem.getQuantity(), productItem.getPrice());
        orderItem.setTotalPrice(totalPrice);
        orderItem.setUnitPrice(productItem.getPrice());
        orderItem.setProduct(productItem);
        orderItem.setOrder(order);

        return orderItem;
    }

    public BigDecimal calculateTotalPrice(int quantity, BigDecimal unitPrice) {
        return unitPrice.multiply(new BigDecimal(quantity));
    }
}
