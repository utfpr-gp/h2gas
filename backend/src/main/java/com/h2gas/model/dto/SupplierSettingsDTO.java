package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

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
        Integer lowStockThreshold,

        // ---- HU17: entrega ----
        @Schema(description = "A loja faz entrega", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Informe se a loja faz entrega.")
        Boolean offersDelivery,

        @Schema(description = "A loja aceita retirada no local", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Informe se a loja aceita retirada.")
        Boolean offersPickup,

        @Schema(description = "Raio de entrega em km (linha reta); nulo = sem limite", example = "8", nullable = true,
                minimum = "0.1", maximum = "100")
        @DecimalMin(value = "0.1", message = "O raio de entrega precisa ser de pelo menos 0,1 km.")
        @DecimalMax(value = "100", message = "O raio de entrega pode ser de no máximo 100 km.")
        BigDecimal deliveryRadiusKm,

        @Schema(description = "Taxa fixa da entrega (R$)", example = "5.00", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Informe a taxa fixa da entrega (0 se não houver).")
        @DecimalMin(value = "0", message = "A taxa fixa não pode ser negativa.")
        BigDecimal deliveryBaseFee,

        @Schema(description = "Valor por km rodado em linha reta (R$)", example = "1.50", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Informe o valor por km (0 se não houver).")
        @DecimalMin(value = "0", message = "O valor por km não pode ser negativo.")
        BigDecimal deliveryFeePerKm
) {
}
