package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Pedidos do mês por modalidade (entrega ou retirada)")
public class DeliveryModeShareDTO {
    @Schema(description = "DELIVERY ou PICKUP (usado no filtro mode)", example = "DELIVERY")
    private String mode;

    @Schema(description = "Pedidos", example = "60")
    private long orders;

    @Schema(description = "Receita", example = "5200.00")
    private BigDecimal revenue;

    @Schema(description = "% dos pedidos", example = "80.0")
    private double percent;
}
