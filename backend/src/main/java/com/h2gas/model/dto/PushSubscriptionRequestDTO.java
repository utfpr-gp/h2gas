package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// HU27: o PushSubscription do navegador (PushSubscription.toJSON())
@Schema(description = "Assinatura de push do navegador")
public record PushSubscriptionRequestDTO(
        @Schema(description = "Endereço do serviço de push do navegador", example = "https://fcm.googleapis.com/fcm/send/abc123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 500) String endpoint,
        @Schema(description = "Chaves de criptografia da assinatura", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid Keys keys
) {
    @Schema(description = "Chaves da assinatura")
    public record Keys(
            @Schema(description = "Chave pública do navegador (base64url)", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 255) String p256dh,
            @Schema(description = "Segredo de autenticação (base64url)", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 255) String auth
    ) {
    }
}
