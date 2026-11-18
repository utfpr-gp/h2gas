package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Categoria do fornecedor logado com o tipo e a quantidade de produtos")
public record SupplierCategoryDTO(
        @Schema(example = "5") Long id,
        @Schema(example = "P13") String name,
        @Schema(description = "Nome do tipo de produto", example = "Gás") String type,
        @Schema(example = "2") Long productTypeId,
        @Schema(description = "Ícone do tipo (HU12)", example = "local_fire_department") String typeIcon,
        @Schema(description = "Produtos cadastrados na categoria", example = "3") long productCount
) {
}
