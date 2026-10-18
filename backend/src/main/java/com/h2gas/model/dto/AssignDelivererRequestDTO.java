package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Entregador escolhido para o despacho do pedido")
public class AssignDelivererRequestDTO {
    @NotNull
    @Schema(description = "ID do entregador da frota do fornecedor", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long delivererId;
}
