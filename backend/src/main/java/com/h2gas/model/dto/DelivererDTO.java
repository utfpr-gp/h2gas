package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Entregador da frota de um fornecedor")
public class DelivererDTO {
    @Schema(description = "ID do entregador", example = "3")
    private Long id;

    @Schema(description = "Nome", example = "Carlos Silva")
    private String name;

    @Schema(description = "E-mail", example = "carlos@h2gas.local")
    private String email;

    @Schema(description = "Telefone", example = "41988887777")
    private String phoneNumber;

    @Schema(description = "Veículo e placa", example = "Moto - ABC-1234")
    private String vehicleInfo;

    @Schema(description = "Se o entregador está ativo na frota")
    private boolean active;

    @Schema(description = "Calculado: true quando há pedido em rota (OUT_FOR_DELIVERY) com este entregador; não é persistido")
    private boolean busy;

    // HU24
    @Schema(description = "Média das notas que os clientes deram ao entregador, 1 casa; nula sem avaliações", example = "4.6", nullable = true)
    private Double averageRating;

    @Schema(description = "Quantas avaliações formam a média do entregador", example = "12")
    private long ratingCount;
}
