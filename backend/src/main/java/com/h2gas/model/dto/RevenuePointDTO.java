package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Receita de um dia do mês")
public class RevenuePointDTO {
    @Schema(description = "Dia", example = "2026-10-05")
    private LocalDate day;

    @Schema(description = "Receita do dia (zero quando não houve venda)", example = "245.00")
    private BigDecimal revenue;
}
