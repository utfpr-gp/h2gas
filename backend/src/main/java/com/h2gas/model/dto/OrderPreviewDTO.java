package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Subtotal calculado pelo backend (RN01); nada é persistido")
public class OrderPreviewDTO {
    @Schema(description = "Itens com preço unitário e total")
    private List<OrderPreviewItemDTO> items;

    @Schema(description = "Soma dos totais dos itens", example = "159.80")
    private BigDecimal subtotal;
}
