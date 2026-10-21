package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Configurações da loja do fornecedor logado")
public record SupplierSettingsDTO(
        @Schema(description = "Abertura (HH:mm)", example = "08:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Informe o horário de abertura.")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Horário de abertura inválido: use HH:mm.")
        String openingTime,

        @Schema(description = "Fechamento (HH:mm); antes da abertura = fecha depois da meia-noite", example = "18:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Informe o horário de fechamento.")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Horário de fechamento inválido: use HH:mm.")
        String closingTime,

        @Schema(description = "Tempo médio de entrega em minutos", example = "30", minimum = "1", maximum = "240",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Informe o tempo médio de entrega.")
        @Min(value = 1, message = "O tempo médio de entrega precisa ser de pelo menos 1 minuto.")
        @Max(value = 240, message = "O tempo médio de entrega pode ser de no máximo 240 minutos.")
        Integer averageDeliveryTime,

        @Schema(description = "Abaixo desta quantidade o produto aparece como estoque baixo", example = "8",
                minimum = "1", maximum = "9999", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Informe o estoque mínimo.")
        @Min(value = 1, message = "O estoque mínimo precisa ser pelo menos 1.")
        @Max(value = 9999, message = "O estoque mínimo pode ser no máximo 9999.")
        Integer lowStockThreshold
) {
}
