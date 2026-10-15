package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemRequestDTO {
    @NotNull
    @Schema(description = "ID do produto", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productItemRef;

    @Positive
    @Schema(description = "Quantidade", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private int quantity;
}
