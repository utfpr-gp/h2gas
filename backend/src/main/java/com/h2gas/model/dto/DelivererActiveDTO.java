package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// HU29: o fornecedor desativa (false) ou reativa (true) um entregador da própria frota
@Schema(description = "Entregador ativo ou desativado")
public record DelivererActiveDTO(
        @Schema(description = "false desativa (sai do despacho e perde o login); true reativa", example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Informe se o entregador fica ativo.")
        Boolean active
) {
}
