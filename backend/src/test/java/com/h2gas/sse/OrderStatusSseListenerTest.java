package com.h2gas.sse;

import com.h2gas.events.OrderStatusChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderStatusSseListenerTest {

    @Test
    @DisplayName("HU08: o evento vira order-status-changed no canal customer:{id}, com orderId e status")
    void devePublicarNoCanalDoClienteComOrderIdEStatus() {
        SseEmitterService sseEmitterService = mock(SseEmitterService.class);
        OrderStatusSseListener listener = new OrderStatusSseListener(sseEmitterService);

        listener.onOrderStatusChanged(new OrderStatusChangedEvent("pedido-1", "cliente-9", "Saiu para entrega"));

        verify(sseEmitterService).publish("customer:cliente-9", "order-status-changed",
                Map.of("orderId", "pedido-1", "status", "Saiu para entrega"));
    }
}
