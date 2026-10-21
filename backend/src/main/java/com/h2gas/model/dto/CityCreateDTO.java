package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Cidade a atender, escolhida na base do IBGE. Nome e UF vêm do IBGE")
public record CityCreateDTO(
        @Schema(description = "Código de município do IBGE (7 dígitos)", example = "4106902", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Escolha o município.")
        Integer ibgeCode
) {
}
