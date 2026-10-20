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
    private Double priceDelivery;
    private Double pricePickup;
    // HU10: contrato do fornecedor, derivado dos dois valores (price = retirada; shippingFee = entrega − retirada)
    private Double price;
    private Double shippingFee;
    // HU10: computado na leitura (priceDelivery == pricePickup); nunca persistido
    private boolean freeDelivery;
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
    private String description;
}
