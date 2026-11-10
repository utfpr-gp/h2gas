package com.h2gas.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Indicadores do fornecedor logado para um mês. Pedido contado = não cancelado, criado no mês")
public class SupplierDashboardDTO {
    @Schema(description = "Mês dos indicadores (YYYY-MM)", example = "2026-10")
    private String month;

    @Schema(description = "HU28: soma do total dos pedidos entregues do mês (o pagamento é na entrega)", example = "4523.00")
    private BigDecimal totalRevenue;

    @Schema(description = "Variação % da receita (entregues) sobre o mês anterior; nulo se o mês anterior não teve receita", example = "12.5")
    private Double revenueChangePercent;

    @Schema(description = "Pedidos contados no mês", example = "84")
    private long totalOrders;

    @Schema(description = "Variação % dos pedidos sobre o mês anterior; nulo se o mês anterior não teve pedidos", example = "14.2")
    private Double ordersChangePercent;

    @Schema(description = "HU28: receita ÷ pedidos entregues; nulo sem entregas", example = "53.85")
    private BigDecimal averageTicket;

    @Schema(description = "Receita dos pedidos contados ainda não entregues", example = "820.00")
    private BigDecimal receivable;

    @Schema(description = "Pedidos contados já entregues", example = "70")
    private long deliveredOrders;

    @Schema(description = "Pedidos pendentes agora (não depende do mês)", example = "3")
    private long pendingOrders;

    @Schema(description = "Pedidos em rota agora (não depende do mês)", example = "2")
    private long outForDeliveryOrders;

    @Schema(description = "Média de deliveredAt − dispatchedAt em minutos; nulo sem entregas com os dois horários", example = "34.5")
    private Double averageDeliveryMinutes;

    // HU24: reputação de todo o histórico, não só do mês
    @Schema(description = "Nota média da loja em todo o histórico, 1 casa; nula sem avaliações", example = "4.3", nullable = true)
    private Double averageRating;

    @Schema(description = "Quantas avaliações formam a nota média da loja", example = "27")
    private long ratingCount;

    @Schema(description = "HU28: receita (pedidos entregues) de cada dia do mês, pelo dia do pedido")
    private List<RevenuePointDTO> revenueByDay;

    @Schema(description = "Top 5 produtos por unidades vendidas")
    private List<ProductSalesDTO> salesByProduct;

    @Schema(description = "Formas de pagamento, da mais usada para a menos")
    private List<PaymentShareDTO> paymentBreakdown;

    @Schema(description = "Vendas por tipo de produto (Gás, Água…), da maior receita para a menor")
    private List<CategorySalesDTO> salesByCategory;

    @Schema(description = "Vendas por categoria da loja (P13, 20L…), da maior receita para a menor")
    private List<StoreCategorySalesDTO> salesByStoreCategory;

    @Schema(description = "Pedidos por modalidade (entrega e retirada), do mais usado para o menos")
    private List<DeliveryModeShareDTO> deliveryModeBreakdown;
}
