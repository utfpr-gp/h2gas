package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Histórico de entregas do entregador logado")
public class DeliveryHistoryDTO {
    @Schema(description = "Entregas concluídas por ele no dia corrente", example = "12")
    private int deliveredToday;

    @Schema(description = "Do evento mais recente ao mais antigo")
    private List<DeliveryHistoryItemDTO> items;
}
