package com.h2gas.model.dto;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Números da plataforma para o painel do administrador")
public class AdminDashboardDto {
    @Schema(description = "Total de usuários por perfil (todos os perfis aparecem, com 0 se vazio)",
            example = "{\"ADMIN\": 1, \"CUSTOMER\": 120, \"SUPPLIER\": 8, \"DELIVERER\": 14}")
    private Map<UserTypeEnum, Long> usersByType;

    @Schema(description = "Fornecedores aprovados", example = "6")
    private long activeSuppliers;

    @Schema(description = "Fornecedores aguardando aprovação", example = "2")
    private long pendingSuppliers;

    @Schema(description = "Cidades atendidas cadastradas", example = "4")
    private long cities;

    @Schema(description = "Pedidos criados hoje", example = "9")
    private long ordersToday;

    @Schema(description = "Pedidos criados no mês corrente", example = "210")
    private long ordersThisMonth;

    @Schema(description = "Total de pedidos por status (todos os pedidos)",
            example = "{\"PENDING\": 3, \"DELIVERED\": 180, \"CANCELED\": 12}")
    private Map<OrderStatusEnum, Long> ordersByStatus;

    @Schema(description = "Soma do total dos pedidos do mês, sem os cancelados", example = "18450.00")
    private BigDecimal revenueThisMonth;
}
