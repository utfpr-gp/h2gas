package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Participação de uma forma de pagamento nos pedidos do mês")
public class PaymentShareDTO {
    @Schema(description = "Forma de pagamento gravada no pedido", example = "PIX")
    private String method;

    @Schema(description = "Pedidos pagos com essa forma", example = "30")
    private long orders;

    @Schema(description = "Receita dessa forma de pagamento", example = "3750.00")
    private BigDecimal revenue;

    @Schema(description = "Percentual sobre os pedidos do mês, 1 casa decimal", example = "68.0")
    private double percent;
}
