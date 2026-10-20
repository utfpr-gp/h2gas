package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Dados para cadastrar ou editar um produto do fornecedor")
public class CreateProductItemDTO {
    @Schema(description = "ID do produto (preenchido só na edição)", example = "12")
    private Long id;

    @NotBlank(message = "Marca é obrigatória")
    @Schema(description = "Marca do produto", example = "Ultragaz", requiredMode = Schema.RequiredMode.REQUIRED)
    private String brand;

    @NotNull(message = "Preço com entrega é obrigatório")
    @DecimalMin(value = "0.0", message = "Preço com entrega não pode ser negativo")
    @Schema(description = "Preço com Entrega (R$)", example = "120.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal priceDelivery;

    @NotNull(message = "Preço para retirada é obrigatório")
    @DecimalMin(value = "0.0", message = "Preço para retirada não pode ser negativo")
    @Schema(description = "Preço para Retirada (R$). Igual ao preço com entrega = Entrega Grátis", example = "100.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal pricePickup;

    @NotNull(message = "Categoria é obrigatória")
    @Schema(description = "Categoria do próprio fornecedor", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryProductId;

    private String imageUrl;

    @Min(value = 0, message = "Estoque não pode ser negativo")
    @Schema(description = "Quantidade em estoque", example = "20")
    private Integer stockQuantity;

    private String description;
}
