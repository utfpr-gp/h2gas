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

    @NotNull(message = "Preço é obrigatório")
    @DecimalMin(value = "0.0", message = "Preço não pode ser negativo")
    @Schema(description = "HU21: preço único do produto (R$), sem frete embutido", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @NotNull(message = "Categoria é obrigatória")
    @Schema(description = "Categoria do produto (global, cadastrada pelo admin)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryProductId;

    private String imageUrl;

    @Min(value = 0, message = "Estoque não pode ser negativo")
    @Schema(description = "Quantidade em estoque", example = "20")
    private Integer stockQuantity;

    private String description;
}
