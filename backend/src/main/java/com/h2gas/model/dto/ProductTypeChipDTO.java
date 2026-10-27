package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// HU15: tipo de produto que a loja vende, exibido como selo no card da vitrine
@Schema(description = "Tipo de produto vendido pela loja, como aparece no selo do card da vitrine")
public record ProductTypeChipDTO(
        @Schema(example = "1") Long id,
        @Schema(example = "Gás") String name,
        @Schema(description = "Material Icon escolhido pelo admin para o tipo", example = "local_fire_department") String icon
) {
}
