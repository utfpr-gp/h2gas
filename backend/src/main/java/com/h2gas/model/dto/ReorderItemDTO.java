package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Item de um pedido antigo pronto para voltar ao carrinho, com dados e preços atuais do produto")
public class ReorderItemDTO {
    @Schema(description = "ID do produto; nulo se o produto foi removido", example = "5")
    private Long productId;

    @Schema(description = "Marca do produto", example = "Ultragaz")
    private String productBrand;

    @Schema(description = "Nome da categoria", example = "P13")
    private String category;

    @Schema(description = "Nome do tipo de produto", example = "Gás")
    private String type;

    @Schema(description = "URL da imagem do produto")
    private String imageUrl;

    @Schema(description = "ID do fornecedor (o checkout cria um pedido por fornecedor)")
    private String supplierId;

    @Schema(description = "Quantidade, limitada ao estoque atual; 0 se indisponível", example = "2")
    private int quantity;

    @Schema(description = "HU21: preço atual do produto", example = "100.00")
    private Double price;

    @Schema(description = "Motivo da indisponibilidade; nulo se disponível",
            allowableValues = {"PRODUTO_REMOVIDO", "SEM_ESTOQUE"})
    private String unavailableReason;
}
