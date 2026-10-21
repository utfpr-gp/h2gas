package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "E-mail novo do admin logado; o código de confirmação vai para ele")
public record EmailChangeRequestDto(
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Schema(description = "E-mail novo", example = "novo.admin@h2gas.local", requiredMode = Schema.RequiredMode.REQUIRED)
        String newEmail) {
}
