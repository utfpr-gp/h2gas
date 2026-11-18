package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Categoria da loja do fornecedor logado (ex.: tamanho ou litragem)")
public record CategoryRequestDTO(
        @Schema(description = "Nome da categoria", example = "P13", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Informe o nome da categoria.")
        @Size(max = 60, message = "O nome da categoria pode ter no máximo 60 caracteres.")
        String name,

        @Schema(description = "Tipo de produto mantido pelo admin", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Escolha o tipo de produto.")
        Long productTypeId
) {
}
