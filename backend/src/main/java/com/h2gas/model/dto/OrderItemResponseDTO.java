package com.h2gas.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemResponseDTO {
    private Long id;
    private Long productId;
    private String productBrand;
    private String productNameCategory;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
