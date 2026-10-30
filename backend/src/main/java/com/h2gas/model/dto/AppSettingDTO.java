package com.h2gas.model.dto;

import com.h2gas.enums.AppSettingKey;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Um interruptor da configuração do sistema")
public record AppSettingDTO(
        @Schema(description = "Qual configuração", example = "WHATSAPP_LOGIN_CODE") AppSettingKey key,
        @Schema(description = "Se está ligada", example = "false") boolean enabled
) {
}
