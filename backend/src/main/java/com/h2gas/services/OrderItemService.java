package com.h2gas.services;

import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.dto.OrderItemResponseDTO;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.OrderItem;
import com.h2gas.model.entity.ProductItem;
import com.h2gas.model.mapper.OrderItemMapper;
import com.h2gas.model.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class OrderItemService {
    @Autowired
    private OrderItemRepository repository;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductItemService productItemService;

    @Autowired
    private OrderItemRepository orderItemRepository;



    public OrderItem createOrderItem(OrderItemRequestDTO orderItemRequestDTO, Order order) {
//        ProductItem productItem = productItemService.findEntityById(orderItemRequestDTO.getProductItemRef());
        ProductItem productItem = productItemService.findEntityByIdWithLock(orderItemRequestDTO.getProductItemRef());
        OrderItem orderItem = orderItemMapper.toRequestEntity(orderItemRequestDTO);

        int quantityRequested = orderItem.getQuantity();
        int currentStock = productItem.getStockQuantity();

        if (quantityRequested > currentStock) {
            throw new BusinessRuleException(
                    "Estoque insuficiente para o produto: " + productItem.getDescription()
            );
        }

        productItem.setStockQuantity(currentStock - quantityRequested);
        productItemService.save(productItem);

        BigDecimal totalPrice = calculateTotalPrice(orderItem.getQuantity(), productItem.getPriceDelivery());
        orderItem.setTotalPrice(totalPrice);
        orderItem.setUnitPrice(productItem.getPriceDelivery());
        orderItem.setProduct(productItem);
        orderItem.setOrder(order);

        return orderItem;
    }

    public Page<OrderItemResponseDTO> findAllOrdersItems(Pageable pageable) {
        return orderItemRepository.findAll(pageable)
                .map(orderItemMapper::toResponseDto);
    }

    public OrderItemResponseDTO findByIdOrderItem(Long id) {
        Optional<OrderItem> orderItemOptional = orderItemRepository.findById(id);
        return orderItemOptional.map(orderItemMapper::toResponseDto).orElseThrow(()
                -> new ResourceNotFoundException("Order Item não encontrado com esse id: " + id));
    }

    private BigDecimal calculateTotalPrice(int quantity, BigDecimal unitPrice) {
        return unitPrice.multiply(new BigDecimal(quantity));
    }
}
