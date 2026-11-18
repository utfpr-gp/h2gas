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
@Schema(description = "Justificativa obrigatória do cancelamento feito pelo fornecedor")
public class CancelOrderRequestDTO {
    @NotBlank
    @Size(max = 500)
    @Schema(description = "Motivo mostrado ao cliente no e-mail de cancelamento", example = "Sem entregador disponível hoje",
            requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 500)
    private String reason;
}
