package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Configuração do sistema vista pelo admin")
public record AppSettingsDTO(
        @Schema(description = "Se o WhatsApp do Twilio está configurado no ambiente", example = "true") boolean whatsappConfigured,
        @Schema(description = "Se o SMS do Twilio está configurado no ambiente (TWILIO_SMS_FROM)", example = "true") boolean smsConfigured,
        @Schema(description = "Os interruptores") List<AppSettingDTO> settings
) {
}
