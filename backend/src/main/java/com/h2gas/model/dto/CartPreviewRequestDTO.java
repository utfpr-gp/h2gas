package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Carrinho do navegador (ids e quantidades) e a modalidade escolhida")
public class CartPreviewRequestDTO {
    @NotEmpty
    @Size(max = 50)
    @Valid
    @ArraySchema(arraySchema = @Schema(description = "Linhas do carrinho, de 1 a 50"), minItems = 1, maxItems = 50)
    private List<CartLineDTO> items;

    @Pattern(regexp = "DELIVERY|PICKUP")
    @Schema(description = "Modalidade; sem valor, vale DELIVERY", example = "DELIVERY",
            allowableValues = {"DELIVERY", "PICKUP"}, nullable = true)
    private String deliveryMode;
}
