package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Dados para o fornecedor cadastrar um entregador da própria frota")
public class DelivererCreateDTO {
    @NotBlank
    @Schema(description = "Nome do entregador", example = "Carlos Silva", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank
    @Email
    @Schema(description = "E-mail de login do entregador", example = "carlos@h2gas.local", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank
    @Schema(description = "Telefone do entregador", example = "41988887777", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;

    @NotBlank
    @Size(min = 6)
    @Schema(description = "Senha inicial (mínimo de 6 caracteres)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Veículo e placa", example = "Moto - ABC-1234")
    private String vehicleInfo;
}
