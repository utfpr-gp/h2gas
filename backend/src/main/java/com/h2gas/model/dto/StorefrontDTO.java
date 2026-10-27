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
                + " vazia quando a loja ainda não cadastrou categorias") List<ProductTypeChipDTO> productTypes
) {
}
