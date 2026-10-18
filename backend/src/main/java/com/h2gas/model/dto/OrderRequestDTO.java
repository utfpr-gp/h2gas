package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDTO {
    private String customerUuid;
    private String supplierRef;
    private String paymentMethod;
    private String note;
    @Schema(description = "Endereço de entrega do cliente; obrigatório só para DELIVERY (ignorado em PICKUP)", example = "12")
    private Long addressId;
    private List<OrderItemRequestDTO> items;

    @Schema(description = "Modalidade logística do pedido; ausente = DELIVERY", example = "DELIVERY",
            allowableValues = {"DELIVERY", "PICKUP"})
    private String deliveryMode;
}
