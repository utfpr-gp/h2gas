package com.h2gas.model.dto;

import com.h2gas.enums.OrderStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDTO {
    private String id;
    private LocalDateTime orderDate;
    private BigDecimal totalPrice;
    // HU17: total = subtotal (itens) + frete; distância nula = sem coordenada (frete só com a taxa fixa)
    @io.swagger.v3.oas.annotations.media.Schema(description = "Soma dos itens", example = "79.90")
    private BigDecimal subtotal;
    @io.swagger.v3.oas.annotations.media.Schema(description = "Frete cobrado (0 na retirada)", example = "9.80")
    private BigDecimal deliveryFee;
    @io.swagger.v3.oas.annotations.media.Schema(description = "Distância em linha reta usada no frete; nula sem coordenada",
            example = "2.4", nullable = true)
    private BigDecimal deliveryDistanceKm;
    private String paymentMethod;
    private String note;
    private LocalDateTime deliveryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String customerName;
    private String supplierName;
    private String customerId;
    private String supplierId;
    private String status;
    private List<OrderItemResponseDTO> orderItems;
    private String streetName;
    private String number;
    private String complement;
    private String neighborhood;
    private String deliveryMode;
    private String customerPhone;
    private String delivererName;
    private String cancellationReason;
    private String cityName;
    private LocalDateTime deliveredAt;
    private String lastRefusalReason;
    private String lastRefusalBy;

    // HU25: para o atalho de WhatsApp
    @Schema(description = "Telefone da loja", example = "4233334444", nullable = true)
    private String supplierPhone;

    @Schema(description = "Telefone do entregador; só vem com o pedido em rota (OUT_FOR_DELIVERY)", example = "42988887777", nullable = true)
    private String delivererPhone;

    // HU24: preenchida na listagem do cliente
    @Schema(description = "Avaliação que o cliente fez do pedido; nula se ainda não avaliou", nullable = true)
    private OrderReviewDTO review;
}
