package com.h2gas.sse;

import com.h2gas.events.OrderPlacedEvent;
import com.h2gas.model.repository.OrderRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * HU27: pedido novo chega na hora para a loja (canal supplier:{id}), só depois do commit — um rollback
 * não pode anunciar um pedido que não foi gravado.
 */
@Component
public class OrderPlacedSseListener {

    static final String EVENT_NAME = "order-placed";

    private final SseEmitterService sseEmitterService;
    private final OrderRepository orderRepository;

    public OrderPlacedSseListener(SseEmitterService sseEmitterService, OrderRepository orderRepository) {
        this.sseEmitterService = sseEmitterService;
        this.orderRepository = orderRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderPlaced(OrderPlacedEvent event) {
        orderRepository.findById(event.orderId())
                .filter(order -> order.getSupplier() != null)
                .ifPresent(order -> sseEmitterService.publish(
                        SseChannels.supplier(order.getSupplier().getId()), EVENT_NAME, Map.of("orderId", order.getId())));
    }
}
