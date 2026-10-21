package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo do estoque dos produtos do fornecedor logado")
public record ProductStockSummaryDTO(
        @Schema(description = "Produtos cadastrados", example = "7") long total,
        @Schema(description = "Com estoque maior que zero", example = "6") long available,
        @Schema(description = "De 1 até o estoque mínimo", example = "2") long lowStock,
        @Schema(description = "Com estoque zero", example = "1") long outOfStock,
        @Schema(description = "Estoque mínimo da configuração da loja", example = "8") int lowStockThreshold
) {
}
