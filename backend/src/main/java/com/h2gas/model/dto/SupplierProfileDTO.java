package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados da loja do fornecedor logado")
public record SupplierProfileDTO(
        @Schema(example = "Gás Centro Curitiba") String name,
        @Schema(description = "E-mail da conta; só leitura (troca pelo suporte)", example = "gascentro@h2gas.local") String email,
        @Schema(example = "41991110000") String phone,
        @Schema(example = "Entrega rápida no centro") String description,
        @Schema(description = "Só leitura", example = "12345678000101") String cnpj,
        @Schema(description = "Logo no Cloudinary; nula sem logo") String logoUrl,
        @Schema(example = "3") Long addressId,
        @Schema(example = "Rua XV de Novembro") String addressStreetName,
        @Schema(example = "700") String addressNumber,
        @Schema(example = "Centro") String addressNeighborhood,
        @Schema(example = "Curitiba") String cityName,
        @Schema(example = "PR") String cityState
) {
}
