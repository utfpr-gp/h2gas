package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// HU13: o que a vitrine pública mostra de uma loja — sem e-mail, telefone, CNPJ nem endereço completo
@Schema(description = "Loja como aparece na vitrine (catálogo e página da loja), inclusive para quem não entrou")
public record StorefrontDTO(
        @Schema(example = "de04de6f-3ac2-44dc-b5aa-04c170d86aa8") String id,
        @Schema(example = "Gás Centro Curitiba") String name,
        @Schema(example = "Botijões P5, P13 e P45 com entrega rápida no centro", nullable = true) String description,
        @Schema(description = "Logo no Cloudinary; nula sem logo", nullable = true) String logoUrl,
        @Schema(description = "Banner no Cloudinary; nulo sem banner", nullable = true) String bannerUrl,
        @Schema(example = "07:00", nullable = true) String openingTime,
        @Schema(example = "20:00", nullable = true) String closingTime,
        @Schema(description = "Tempo médio de entrega em minutos", example = "60", nullable = true) Long averageDeliveryTime,
        @Schema(description = "HU18: aberta agora pelo horário de funcionamento, na hora de Brasília") boolean open,
        @Schema(example = "3") Long cityId,
        @Schema(example = "Curitiba") String cityName,
        @Schema(example = "Centro", nullable = true) String addressNeighborhood,
        @Schema(description = "Média das avaliações, 1 casa; nula sem avaliações", example = "4.2", nullable = true) Double averageRating,
        @Schema(description = "Quantidade de avaliações", example = "14") Long reviewCount,
        @Schema(description = "HU15: tipos de produto que a loja vende, sem repetição e em ordem alfabética;"
                + " vazia quando a loja ainda não cadastrou categorias") List<ProductTypeChipDTO> productTypes,
        // ---- HU17: entrega e frete por km (calculados no backend, RN01) ----
        @Schema(description = "A loja faz entrega") boolean offersDelivery,
        @Schema(description = "A loja aceita retirada no local") boolean offersPickup,
        @Schema(description = "Distância em linha reta até o CEP; nula sem CEP ou sem coordenada", example = "2.4", nullable = true)
        java.math.BigDecimal distanceKm,
        @Schema(description = "Frete até o CEP; nulo sem CEP ou quando a loja não entrega ali", example = "9.80", nullable = true)
        java.math.BigDecimal deliveryFee,
        @Schema(description = "Se a loja entrega no CEP (false = só retirada); nulo sem CEP", nullable = true) Boolean deliversHere,
        @Schema(description = "Entrega grátis: taxa fixa e valor por km zerados") boolean freeDelivery
) {
}
