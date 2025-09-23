package com.h2gas.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateProductItemDTO {
    private Long id;
    private String brand;
    private BigDecimal price;
    private Long categoryProductId;
    private Long supplierId;
    private String imageUrl;
    private Integer stockQuantity;
}
