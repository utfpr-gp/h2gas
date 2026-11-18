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

    // ---- HU17: frete por km ----
    @Schema(description = "Frete até o CEP; 0 na retirada; nulo sem CEP ou quando a loja não entrega ali", nullable = true, example = "9.80")
    private BigDecimal deliveryFee;

    @Schema(description = "Distância em linha reta; nula sem coordenada (o frete é só a taxa fixa)", nullable = true, example = "2.4")
    private BigDecimal distanceKm;

    @Schema(description = "Se a loja entrega no CEP; nulo na retirada ou sem CEP", nullable = true, example = "true")
    private Boolean deliversHere;

    @Schema(description = "Subtotal + frete (quando há frete calculado)", example = "229.80")
    private BigDecimal total;

    public CartPreviewDTO(String supplierId, String supplierName, List<CartPreviewItemDTO> items, BigDecimal subtotal) {
        this(supplierId, supplierName, items, subtotal, null, null, null, subtotal);
    }
}
