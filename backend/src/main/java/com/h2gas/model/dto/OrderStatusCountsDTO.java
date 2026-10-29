package com.h2gas.model.dto;

import com.h2gas.enums.OrderStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

// HU23: quantos pedidos a loja tem em cada status (todos os status, 0 quando não há)
@Schema(description = "Contagem dos pedidos da loja por status")
public record OrderStatusCountsDTO(
        @Schema(description = "Pedidos por status (PENDING, CONFIRMED, IN_PREPARATION, OUT_FOR_DELIVERY, DELIVERED, CANCELED)",
                example = "{\"PENDING\": 2, \"CONFIRMED\": 1, \"IN_PREPARATION\": 0, \"OUT_FOR_DELIVERY\": 1, \"DELIVERED\": 7, \"CANCELED\": 1}")
        Map<OrderStatusEnum, Long> counts,
        @Schema(description = "Em andamento: nem entregues nem cancelados", example = "4") long active,
        @Schema(description = "Todos os pedidos da loja", example = "12") long total
) {
}
