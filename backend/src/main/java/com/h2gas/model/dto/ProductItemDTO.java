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
    // HU15 §5.5: o card de produto mostra a logo redonda da loja que vende
    @io.swagger.v3.oas.annotations.media.Schema(description = "Logo da loja que vende (Cloudinary); nula sem logo",
            nullable = true, example = "https://res.cloudinary.com/h2gas/image/upload/logo.png")
    private String supplierLogoUrl;
    private Long categoryProductId;
//    private SupplierDTO supplier;
    private String imageUrl;
    private Integer stockQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String description;
}
