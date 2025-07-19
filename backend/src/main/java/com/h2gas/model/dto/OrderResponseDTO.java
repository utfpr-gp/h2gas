package com.h2gas.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDTO {
    private String id;
    private LocalDateTime orderDate;
    private BigDecimal totalPrice;
    private String paymentMethod;
    private String note;
    private LocalDateTime deliveryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String customerName;
    private String distributorName;
    private String customerId;
    private String distributorId;
    private List<OrderItemResponseDTO> orderItems;
}
