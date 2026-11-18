package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Resumo do histórico de pedidos finalizados do fornecedor logado, com os mesmos filtros da lista")
public record OrderHistorySummaryDTO(
        @Schema(description = "Pedidos entregues", example = "70") long delivered,
        @Schema(description = "Pedidos cancelados", example = "4") long canceled,
        @Schema(description = "Soma do total dos entregues", example = "4523.00") BigDecimal revenue,
        @Schema(description = "Receita ÷ entregues; nulo sem entregues", example = "64.61") BigDecimal averageTicket
) {
}
