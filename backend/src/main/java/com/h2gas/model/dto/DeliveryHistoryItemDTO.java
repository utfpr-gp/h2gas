package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Uma entrega concluída ou recusada pelo entregador")
public class DeliveryHistoryItemDTO {
    @Schema(description = "ID do pedido")
    private String orderId;

    @Schema(description = "DELIVERED (entregue por ele) ou REFUSED (recusada por ele)", example = "DELIVERED")
    private String kind;

    @Schema(description = "Quando foi entregue (deliveredAt) ou recusada")
    private LocalDateTime occurredAt;

    private String streetName;
    private String number;
    private String neighborhood;
    private String cityName;

    @Schema(description = "Itens do pedido em uma linha", example = "2x Ultragaz Gás 13kg, 1x Água 20L")
    private String itemsSummary;

    private String paymentMethod;

    @Schema(description = "Total do pedido; nulo quando a entrega foi recusada")
    private BigDecimal totalPrice;
}
