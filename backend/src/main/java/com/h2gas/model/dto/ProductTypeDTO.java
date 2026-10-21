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
@Schema(description = "Tipo de produto da plataforma (ex.: Gás, Água); o fornecedor cria as categorias dentro dele")
public class ProductTypeDTO {
    @Schema(description = "Id do tipo", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank
    @Size(max = 60)
    @Schema(description = "Nome do tipo, único sem diferenciar maiúsculas", example = "Gás",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}
