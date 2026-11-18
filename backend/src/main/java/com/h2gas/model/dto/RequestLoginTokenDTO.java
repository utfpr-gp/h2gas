package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "E-mail que receberá o código de acesso")
public class RequestLoginTokenDTO {
    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Schema(description = "E-mail cadastrado", example = "cliente@h2gas.local", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    // HU26: nulo = e-mail
    @Schema(description = "Por onde enviar o código; PHONE vai para o celular cadastrado (WhatsApp ou SMS, conforme o admin) e volta para o e-mail quando não dá",
            example = "EMAIL", defaultValue = "EMAIL", nullable = true)
    private com.h2gas.enums.LoginChannelEnum channel;
}
