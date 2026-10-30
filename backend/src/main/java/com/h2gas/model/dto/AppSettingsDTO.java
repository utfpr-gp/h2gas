package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Configuração do sistema vista pelo admin")
public record AppSettingsDTO(
        @Schema(description = "Se as credenciais do Twilio estão no ambiente; sem elas nada de WhatsApp pode ser ligado", example = "true")
        boolean whatsappConfigured,
        @Schema(description = "Os interruptores") List<AppSettingDTO> settings
) {
}
