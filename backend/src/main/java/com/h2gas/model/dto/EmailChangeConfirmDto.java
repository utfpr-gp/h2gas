package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Código recebido no e-mail novo")
public record EmailChangeConfirmDto(
        @NotBlank(message = "Código é obrigatório")
        @Pattern(regexp = "\\d{6}", message = "Código deve ter 6 dígitos")
        @Schema(description = "Código de 6 dígitos", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
        String code) {
}
