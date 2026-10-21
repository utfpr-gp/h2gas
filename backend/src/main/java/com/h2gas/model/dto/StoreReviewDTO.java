package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// F7 §5.27: avaliação mostrada ao cliente; só o primeiro nome de quem avaliou (sem sobrenome, e-mail ou telefone)
@Schema(description = "Avaliação de uma loja, como o cliente vê na lista de avaliações")
public record StoreReviewDTO(
        @Schema(example = "12") Long id,
        @Schema(description = "Primeiro nome do cliente que avaliou", example = "Maria") String customerFirstName,
        @Schema(description = "Nota de 1 a 5", example = "4", minimum = "1", maximum = "5") Integer rating,
        @Schema(description = "Comentário; nulo quando o cliente não escreveu", example = "Entrega rápida, recomendo.", nullable = true) String comment,
        @Schema(description = "Data da entrega do pedido avaliado; nula se o pedido não tiver", example = "2026-10-18T14:30:00", nullable = true)
        LocalDateTime reviewedAt
) {
}
