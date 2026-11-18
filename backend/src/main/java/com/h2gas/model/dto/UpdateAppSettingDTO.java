package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Liga ou desliga uma configuração")
public record UpdateAppSettingDTO(
        @Schema(description = "Ligada ou não", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Informe se fica ligada.") Boolean enabled
) {
}
