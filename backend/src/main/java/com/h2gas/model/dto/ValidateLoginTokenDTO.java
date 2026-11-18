package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "E-mail e código de acesso recebido por e-mail")
public class ValidateLoginTokenDTO {
    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Schema(description = "E-mail que pediu o código", example = "cliente@h2gas.local", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Código é obrigatório")
    @Pattern(regexp = "\\d{6}", message = "Código deve ter 6 dígitos")
    @Schema(description = "Código de 6 dígitos", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}
