package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Chave pública VAPID para o navegador assinar o push")
public record PushPublicKeyDTO(
        @Schema(description = "Chave pública VAPID (base64url); vazia sem configuração", example = "BEl62iUYgUivxIkv69yViEuiBIa...")
        String publicKey,
        @Schema(description = "Se o push está configurado no servidor", example = "true") boolean configured
) {
}
