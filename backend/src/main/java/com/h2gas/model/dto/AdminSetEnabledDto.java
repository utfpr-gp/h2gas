package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Ativa ou desativa a conta de um usuário")
public class AdminSetEnabledDto {
    @NotNull
    @Schema(description = "true reativa, false desativa", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean enabled;
}
