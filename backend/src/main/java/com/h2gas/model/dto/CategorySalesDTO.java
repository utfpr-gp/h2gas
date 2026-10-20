package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Vendas de uma categoria (tipo de produto) no mês")
public class CategorySalesDTO {
    @Schema(description = "Tipo de produto", example = "Gás")
    private String categoryName;

    @Schema(description = "Unidades vendidas", example = "58")
    private long quantity;

    @Schema(description = "Receita dos itens da categoria", example = "7250.00")
    private BigDecimal revenue;

    @Schema(description = "Percentual da receita dos itens do mês, 1 casa decimal", example = "62.5")
    private double percent;
}
