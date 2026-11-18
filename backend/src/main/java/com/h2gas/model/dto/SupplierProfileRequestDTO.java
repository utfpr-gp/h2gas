package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados da loja que o fornecedor edita (horário e entrega ficam na configuração)")
public record SupplierProfileRequestDTO(
        @Schema(description = "Nome da loja", example = "Gás Centro Curitiba", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Informe o nome da loja.")
        @Size(max = 80, message = "O nome da loja pode ter no máximo 80 caracteres.")
        String name,

        @Schema(description = "Telefone só com dígitos (DDD + número)", example = "41991110000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Informe o telefone.")
        @Pattern(regexp = "^\\d{10,11}$", message = "Telefone inválido: use DDD e número, só dígitos.")
        String phone,

        @Schema(description = "Descrição curta para os clientes", example = "Entrega rápida no centro")
        @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres.")
        String description
) {
}
