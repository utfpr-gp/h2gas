package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Motivo obrigatório da recusa da entrega pelo entregador")
public class CancelDeliveryRequestDTO {
    @NotBlank
    @Size(max = 500)
    @Schema(description = "Motivo mostrado ao fornecedor no painel", example = "Moto quebrou",
            requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 500)
    private String reason;
}
