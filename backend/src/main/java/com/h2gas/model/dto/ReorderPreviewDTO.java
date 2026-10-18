package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Recompra de um pedido: itens que podem voltar ao carrinho e itens que ficaram de fora")
public class ReorderPreviewDTO {
    @Schema(description = "Itens disponíveis, com quantidade limitada ao estoque atual")
    private List<ReorderItemDTO> availableItems;

    @Schema(description = "Itens removidos ou sem estoque")
    private List<ReorderItemDTO> unavailableItems;
}
