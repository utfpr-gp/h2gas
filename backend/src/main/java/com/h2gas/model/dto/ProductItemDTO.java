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
    @io.swagger.v3.oas.annotations.media.Schema(description = "F12: código público do produto, usado na URL da edição",
            example = "k3f9x2ab01")
    private String publicId;
    private String brand;
    // HU21: preço único do produto; o frete vem da distância (HU17)
    @io.swagger.v3.oas.annotations.media.Schema(description = "Preço do produto (R$), igual na entrega e na retirada", example = "100.00")
    private Double price;
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
