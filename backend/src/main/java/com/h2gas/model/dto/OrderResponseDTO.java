package com.h2gas.model.dto;

import com.h2gas.enums.OrderStatusEnum;
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
    private String supplierName;
    private String customerId;
    private String supplierId;
    private String status;
    private List<OrderItemResponseDTO> orderItems;
    private String streetName;
    private String number;
    private String complement;
    private String neighborhood;
    private String deliveryMode;
    private String customerPhone;
    private String delivererName;
    private String cancellationReason;
    private String cityName;
    private LocalDateTime deliveredAt;
    private String lastRefusalReason;
    private String lastRefusalBy;
}
