package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Vendas de uma categoria da loja (ex.: P13, 20L) no mês")
public class StoreCategorySalesDTO {
    @Schema(description = "Id da categoria (usado no filtro categoryId)", example = "7")
    private Long categoryId;

    @Schema(description = "Nome da categoria", example = "Gás 13kg")
    private String categoryName;

    @Schema(description = "Tipo de produto da categoria", example = "Gás")
    private String typeName;

    @Schema(description = "Unidades vendidas", example = "58")
    private long quantity;

    @Schema(description = "Receita da categoria", example = "7250.00")
    private BigDecimal revenue;

    @Schema(description = "% da receita dos itens contados", example = "62.5")
    private double percent;
}
