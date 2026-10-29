package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// HU22: senha nova que o fornecedor define para um entregador da própria frota
@Schema(description = "Senha nova do entregador")
public record DelivererPasswordDTO(
        @Schema(description = "Senha nova, com pelo menos 6 caracteres", example = "novaSenha9",
                minLength = 6, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Informe a senha nova.")
        @Size(min = 6, message = "A senha precisa ter pelo menos 6 caracteres.")
        String password
) {
}
