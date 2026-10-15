package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Item do preview com o preço da modalidade escolhida")
public class OrderPreviewItemDTO {
    @Schema(description = "ID do produto", example = "5")
    private Long productItemRef;

    @Schema(description = "Quantidade", example = "2")
    private int quantity;

    @Schema(description = "Preço unitário na modalidade informada", example = "79.90")
    private BigDecimal unitPrice;

    @Schema(description = "Preço unitário × quantidade", example = "159.80")
    private BigDecimal totalPrice;
}
