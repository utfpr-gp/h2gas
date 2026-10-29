package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// HU24: avaliação que o cliente fez do pedido
@Schema(description = "Avaliação feita pelo cliente para o pedido")
public record OrderReviewDTO(
        @Schema(description = "Nota da loja, de 1 a 5", example = "5") Integer storeRating,
        @Schema(description = "Nota do entregador; nula na retirada ou sem nota", example = "4", nullable = true) Integer delivererRating,
        @Schema(description = "Comentário; nulo sem comentário", example = "Entrega rápida, recomendo.", nullable = true) String comment
) {
}
