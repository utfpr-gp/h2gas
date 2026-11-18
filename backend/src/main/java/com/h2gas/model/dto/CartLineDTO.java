package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Linha do carrinho guardada no navegador: só o produto e a quantidade")
public class CartLineDTO {
    @NotNull
    @Schema(description = "ID do produto", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @NotNull
    @Min(1)
    @Schema(description = "Quantidade (mínimo 1)", example = "2", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;
}
