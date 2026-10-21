package com.h2gas.model.dto;

import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.exceptions.BusinessRuleException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// F7 fatia 6: filtros do histórico de pedidos finalizados do fornecedor; o período vale pela criação do pedido
public record OrderHistoryFilter(
        List<OrderStatusEnum> statuses,
        LocalDateTime from,
        LocalDateTime toExclusive,
        String payment,
        DeliveryModeEnum mode,
        String q
) {
    private static final Set<String> PAYMENTS = Set.of("PIX", "DINHEIRO", "CREDIT_CARD");

    public static OrderHistoryFilter parse(String status, String from, String to, String payment, String mode, String q) {
        List<OrderStatusEnum> statuses = switch (blank(status) ? "" : status.trim().toLowerCase(Locale.ROOT)) {
            case "" -> List.of(OrderStatusEnum.DELIVERED, OrderStatusEnum.CANCELED);
            case "delivered" -> List.of(OrderStatusEnum.DELIVERED);
            case "canceled" -> List.of(OrderStatusEnum.CANCELED);
            default -> throw new BusinessRuleException("Status inválido: use delivered ou canceled.");
        };
        LocalDate fromDate = date(from);
        LocalDate toDate = date(to);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessRuleException("A data inicial precisa ser anterior ou igual à final.");
        }
        String pay = blank(payment) ? null : payment.trim().toUpperCase(Locale.ROOT);
        if (pay != null && !PAYMENTS.contains(pay)) {
            throw new BusinessRuleException("Forma de pagamento inválida: use PIX, DINHEIRO ou CREDIT_CARD.");
        }
        DeliveryModeEnum deliveryMode;
        try {
            deliveryMode = blank(mode) ? null : DeliveryModeEnum.valueOf(mode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Modalidade inválida: use DELIVERY ou PICKUP.");
        }
        return new OrderHistoryFilter(statuses,
                fromDate == null ? null : fromDate.atStartOfDay(),
                toDate == null ? null : toDate.plusDays(1).atStartOfDay(),
                pay, deliveryMode, blank(q) ? null : q.trim().toLowerCase(Locale.ROOT));
    }

    private static LocalDate date(String value) {
        if (blank(value)) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException("Data inválida: use o formato AAAA-MM-DD.");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
