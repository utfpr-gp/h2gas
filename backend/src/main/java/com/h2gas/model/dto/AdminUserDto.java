package com.h2gas.model.dto;

import com.h2gas.enums.UserTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Usuário na gestão do administrador")
public class AdminUserDto {
    @Schema(description = "Id do usuário", example = "6f1c2b9e-3a1d-4c55-9a43-0f1e2d3c4b5a")
    private String id;

    @Schema(description = "Nome", example = "Maria Souza")
    private String name;

    @Schema(description = "E-mail de login", example = "maria@h2gas.local")
    private String email;

    @Schema(description = "Telefone", example = "46999990000")
    private String phoneNumber;

    @Schema(description = "Perfil", example = "CUSTOMER")
    private UserTypeEnum userType;

    @Schema(description = "Conta ativa; false = desativada pelo admin (não entra por senha nem por código)", example = "true")
    private boolean enabled;
}
