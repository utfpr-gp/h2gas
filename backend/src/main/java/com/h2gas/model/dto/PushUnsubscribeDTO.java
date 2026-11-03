package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Assinatura a apagar")
public record PushUnsubscribeDTO(
        @Schema(description = "Endereço da assinatura", example = "https://fcm.googleapis.com/fcm/send/abc123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String endpoint
) {
}
