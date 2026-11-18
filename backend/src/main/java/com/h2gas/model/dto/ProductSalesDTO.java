package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Vendas de um produto no mês")
public class ProductSalesDTO {
    @Schema(description = "Id do produto (usado no filtro productId)", example = "4")
    private Long productId;

    @Schema(description = "Marca e categoria do produto", example = "Ultragaz Gás 13kg")
    private String productName;

    @Schema(description = "Unidades vendidas", example = "58")
    private long quantity;

    @Schema(description = "Receita do produto", example = "7250.00")
    private BigDecimal revenue;
}
