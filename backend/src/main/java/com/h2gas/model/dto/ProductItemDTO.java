package com.h2gas.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductItemDTO {
    private Long id;
    private String brand;
    private Double price;
    private String category;
    private String type;
    private String supllierName;
    private String supplierId;
    private Long categoryProductId;
//    private SupplierDTO supplier;
    private String imageUrl;
    private Integer stockQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
