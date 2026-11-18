package com.h2gas.services;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.model.entity.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.OptionalLong;

// HU28: pedido parado por etapa, no horário de Brasília (Clock). Pendente > 15 min, confirmado ou em
// preparo > 1 h, em rota > 2 h, ou qualquer pedido em aberto feito em outro dia.
@Component
public class OrderStallPolicy {

    private static final Duration PENDING_LIMIT = Duration.ofMinutes(15);
    private static final Duration PREPARING_LIMIT = Duration.ofHours(1);
    private static final Duration ON_ROUTE_LIMIT = Duration.ofHours(2);

    private final Clock clock;

    public OrderStallPolicy(Clock clock) {
        this.clock = clock;
    }

    /** Minutos na etapa atual quando o pedido está parado; vazio quando não está. */
    public OptionalLong stalledMinutes(Order order) {
        OrderStatusEnum status = order.getStatus();
        if (status == null || status == OrderStatusEnum.DELIVERED || status == OrderStatusEnum.CANCELED
                || order.getCreatedAt() == null) {
            return OptionalLong.empty();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime stageStart = stageStart(order);
        Duration inStage = Duration.between(stageStart, now);
        boolean fromAnotherDay = order.getCreatedAt().toLocalDate().isBefore(LocalDate.now(clock));
        if (fromAnotherDay || inStage.compareTo(limitFor(status)) > 0) {
            return OptionalLong.of(Math.max(0, inStage.toMinutes()));
        }
        return OptionalLong.empty();
    }

    private LocalDateTime stageStart(Order order) {
        LocalDateTime start = switch (order.getStatus()) {
            case PENDING -> order.getCreatedAt();
            case OUT_FOR_DELIVERY -> order.getDispatchedAt() != null ? order.getDispatchedAt() : order.getUpdatedAt();
            default -> order.getUpdatedAt();
        };
        return start != null ? start : order.getCreatedAt();
    }

    private Duration limitFor(OrderStatusEnum status) {
        return switch (status) {
            case PENDING -> PENDING_LIMIT;
            case OUT_FOR_DELIVERY -> ON_ROUTE_LIMIT;
            default -> PREPARING_LIMIT;
        };
    }
}
