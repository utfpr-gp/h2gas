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
    private Long addressId;
    private List<OrderItemRequestDTO> items;

    @Schema(description = "Modalidade logística do pedido; ausente = DELIVERY", example = "DELIVERY",
            allowableValues = {"DELIVERY", "PICKUP"})
    private String deliveryMode;
}
