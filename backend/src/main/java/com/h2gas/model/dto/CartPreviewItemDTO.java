package com.h2gas.model.dto;

import com.h2gas.enums.CartUnavailableReasonEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Item do carrinho com os dados e o preço atuais do produto")
public class CartPreviewItemDTO {
    @Schema(description = "ID do produto", example = "5")
    private Long productId;

    @Schema(description = "Marca; nula se o produto saiu do catálogo", example = "Ultragaz", nullable = true)
    private String brand;

    @Schema(description = "Categoria", example = "Gás 13kg", nullable = true)
    private String category;

    @Schema(description = "Tipo do produto", example = "Gás", nullable = true)
    private String type;

    @Schema(description = "Foto do produto", nullable = true, example = "https://res.cloudinary.com/h2gas/image/upload/p.png")
    private String imageUrl;

    @Schema(description = "Quantidade pedida", example = "2")
    private int quantity;

    @Schema(description = "Estoque atual, limite da quantidade", example = "12", nullable = true)
    private Integer stockQuantity;

    @Schema(description = "Preço unitário na modalidade", example = "110.00", nullable = true)
    private BigDecimal unitPrice;

    @Schema(description = "Preço unitário × quantidade", example = "220.00", nullable = true)
    private BigDecimal totalPrice;

    @Schema(description = "Se o item pode ser comprado agora", example = "true")
    private boolean available;

    @Schema(description = "Motivo da indisponibilidade; nulo quando disponível", nullable = true,
            example = "OUT_OF_STOCK")
    private CartUnavailableReasonEnum unavailableReason;
}
