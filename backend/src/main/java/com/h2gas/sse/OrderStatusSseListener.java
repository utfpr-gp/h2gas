package com.h2gas.sse;

import com.h2gas.events.OrderStatusChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * HU08: leva a mudança de status ao canal do cliente (customer:{id}) só depois do commit — um
 * rollback não pode anunciar um status que não foi gravado. fallbackExecution cobre chamadas
 * fora de transação.
 */
@Component
public class OrderStatusSseListener {

    static final String EVENT_NAME = "order-status-changed";

    private final SseEmitterService sseEmitterService;

    public OrderStatusSseListener(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        sseEmitterService.publish(
                SseChannels.customer(event.customerId()),
                EVENT_NAME,
                Map.of("orderId", event.orderId(), "status", event.status()));
    }
}
