package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Canais disponíveis para receber o código de login")
public record LoginChannelsDTO(
        @Schema(description = "Se dá para receber o código pelo WhatsApp (ligado pelo admin e Twilio configurado)", example = "false")
        boolean whatsapp
) {
}
