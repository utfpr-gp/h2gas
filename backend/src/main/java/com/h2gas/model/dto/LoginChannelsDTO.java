package com.h2gas.model.dto;

import com.h2gas.enums.PhoneChannel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Canais disponíveis para receber o código de login")
public record LoginChannelsDTO(
        @Schema(description = "Se dá para receber o código no celular (ligado pelo admin e Twilio configurado)", example = "false")
        boolean phone,
        @Schema(description = "Como a mensagem vai ao celular", example = "SMS") PhoneChannel phoneChannel
) {
}
