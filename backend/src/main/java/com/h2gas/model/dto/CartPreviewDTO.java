package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Carrinho com dados, preços e disponibilidade atuais, calculados no backend (RN01)")
public class CartPreviewDTO {
    @Schema(description = "Loja do carrinho; nula se nenhum produto foi encontrado", nullable = true, example = "3f2a…")
    private String supplierId;

    @Schema(description = "Nome da loja", nullable = true, example = "Gás Centro Curitiba")
    private String supplierName;

    @Schema(description = "Itens, na ordem enviada")
    private List<CartPreviewItemDTO> items;

    @Schema(description = "Soma dos itens disponíveis", example = "220.00")
    private BigDecimal subtotal;
}
