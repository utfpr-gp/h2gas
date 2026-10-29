package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// HU24: avaliação que o cliente envia para um pedido entregue
@Schema(description = "Avaliação de um pedido entregue: nota da loja, nota do entregador e comentário")
public record CreateReviewDTO(
        @Schema(description = "Nota da loja, de 1 a 5", example = "5", minimum = "1", maximum = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Dê uma nota para a loja.") @Min(value = 1, message = "A nota vai de 1 a 5.") @Max(value = 5, message = "A nota vai de 1 a 5.")
        Integer storeRating,

        @Schema(description = "Nota do entregador, de 1 a 5; só para pedido com entregador", example = "4", minimum = "1", maximum = "5", nullable = true)
        @Min(value = 1, message = "A nota vai de 1 a 5.") @Max(value = 5, message = "A nota vai de 1 a 5.")
        Integer delivererRating,

        @Schema(description = "Comentário opcional, até 500 caracteres", example = "Entrega rápida, recomendo.", nullable = true, maxLength = 500)
        @Size(max = 500, message = "O comentário tem no máximo 500 caracteres.")
        String comment
) {
}
