package com.h2gas.model.dto;

import com.h2gas.enums.UserTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Dados para o administrador criar um usuário de qualquer perfil")
public class AdminCreateUserDto {
    @NotBlank
    @Size(min = 3)
    @Schema(description = "Nome", example = "Ana Admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank
    @Email
    @Schema(description = "E-mail de login (único)", example = "ana@h2gas.local", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank
    @Size(min = 6)
    @Schema(description = "Senha inicial (mínimo de 6 caracteres)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank
    @Schema(description = "Telefone", example = "46999990000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;

    @Schema(description = "CPF (opcional)", example = "12345678900")
    private String cpf;

    @NotNull
    @Schema(description = "Perfil do novo usuário (inclui ADMIN)", example = "SUPPLIER", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserTypeEnum userType;

    @Schema(description = "CNPJ — obrigatório para SUPPLIER", example = "12345678000100")
    private String cnpj;

    @Schema(description = "Cidade de atendimento — obrigatória para SUPPLIER", example = "1")
    private Long cityId;

    @Schema(description = "Fornecedor dono da frota — obrigatório para DELIVERER", example = "6f1c2b9e-3a1d-4c55-9a43-0f1e2d3c4b5a")
    private String supplierId;
}
