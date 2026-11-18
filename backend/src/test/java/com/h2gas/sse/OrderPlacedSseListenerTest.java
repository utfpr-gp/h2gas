package com.h2gas.sse;

import com.h2gas.events.OrderPlacedEvent;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

// HU27: o pedido novo vira order-placed no canal da loja, depois do commit
class OrderPlacedSseListenerTest {

    @Test
    void deveAvisarALojaDoPedidoNovo() {
        SseEmitterService sse = mock(SseEmitterService.class);
        OrderRepository orders = mock(OrderRepository.class);
        Supplier loja = new Supplier();
        loja.setId("loja-7");
        Order pedido = new Order();
        pedido.setId("c6423ce8-fc0b");
        pedido.setSupplier(loja);
        when(orders.findById("c6423ce8-fc0b")).thenReturn(Optional.of(pedido));

        new OrderPlacedSseListener(sse, orders).onOrderPlaced(new OrderPlacedEvent("c6423ce8-fc0b"));

        verify(sse).publish("supplier:loja-7", "order-placed", Map.of("orderId", "c6423ce8-fc0b"));
    }

    @Test
    void naoDeveFalharSeOPedidoSumiu() {
        SseEmitterService sse = mock(SseEmitterService.class);
        OrderRepository orders = mock(OrderRepository.class);
        when(orders.findById("x")).thenReturn(Optional.empty());

        new OrderPlacedSseListener(sse, orders).onOrderPlaced(new OrderPlacedEvent("x"));

        verifyNoInteractions(sse);
    }
}
