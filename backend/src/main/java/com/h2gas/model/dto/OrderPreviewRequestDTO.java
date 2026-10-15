package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Itens do carrinho e modalidade para pré-visualizar o subtotal")
public class OrderPreviewRequestDTO {
    @NotEmpty
    @Valid
    @Schema(description = "Itens do carrinho", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OrderItemRequestDTO> items;

    @NotBlank
    @Schema(description = "Modalidade logística do pedido", example = "PICKUP",
            allowableValues = {"DELIVERY", "PICKUP"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String deliveryMode;
}
