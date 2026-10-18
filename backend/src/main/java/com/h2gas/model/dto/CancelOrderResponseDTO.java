package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Resultado do cancelamento")
public class CancelOrderResponseDTO {
    @Schema(description = "Novo status do pedido", example = "Cancelado")
    private String status;

    @Schema(description = "Se o e-mail de aviso ao cliente foi enviado; false não desfaz o cancelamento")
    private boolean customerNotified;
}
