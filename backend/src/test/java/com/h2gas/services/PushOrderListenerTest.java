package com.h2gas.services;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.events.DelivererAssignedEvent;
import com.h2gas.events.OrderPlacedEvent;
import com.h2gas.events.OrderStatusChangedEvent;
import com.h2gas.model.entity.Deliverer;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// HU27: quem é avisado em cada evento do pedido
class PushOrderListenerTest {

    private final PushNotificationService push = mock(PushNotificationService.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final PushOrderListener listener = new PushOrderListener(push, orders);

    @BeforeEach
    void setUp() {
        Supplier loja = new Supplier();
        loja.setId("loja-1");
        loja.setName("Gás Guará Centro");
        Deliverer entregador = new Deliverer();
        entregador.setUser(new User("ent-1", "João Entregador", "j@teste.com", "42900000000", null, "s", UserTypeEnum.DELIVERER));
        Order o = new Order();
        o.setId("94c103db-8f0c");
        o.setCustomer(new User("cli-1", "Maria", "m@teste.com", "42990000011", null, "s", UserTypeEnum.CUSTOMER));
        o.setSupplier(loja);
        o.setDeliverer(entregador);
        o.setTotalPrice(new BigDecimal("111.96"));
        when(orders.findById("94c103db-8f0c")).thenReturn(Optional.of(o));
    }

    @Test
    void deveAvisarOClienteNosStatus() {
        listener.onStatusChanged(new OrderStatusChangedEvent("94c103db-8f0c", "cli-1", "Saiu para entrega"));
        listener.onStatusChanged(new OrderStatusChangedEvent("94c103db-8f0c", "cli-1", "Confirmado"));
        listener.onStatusChanged(new OrderStatusChangedEvent("94c103db-8f0c", "cli-1", "Entregue"));
        listener.onStatusChanged(new OrderStatusChangedEvent("94c103db-8f0c", "cli-1", "Cancelado"));

        verify(push).notifyUser("cli-1", "Pedido #94c103db saiu para entrega",
                "João está a caminho com o seu pedido da Gás Guará Centro.", "/meus-pedidos");
        verify(push).notifyUser("cli-1", "Pedido #94c103db confirmado",
                "A Gás Guará Centro confirmou o seu pedido.", "/meus-pedidos");
        verify(push).notifyUser("cli-1", "Pedido #94c103db entregue",
                "Bom proveito! Conte como foi em Meus pedidos.", "/meus-pedidos");
        verify(push).notifyUser("cli-1", "Pedido #94c103db cancelado",
                "A Gás Guará Centro cancelou o pedido. Veja o motivo em Meus pedidos.", "/meus-pedidos");
    }

    @Test
    void naoDeveAvisarEmPreparo() {
        listener.onStatusChanged(new OrderStatusChangedEvent("94c103db-8f0c", "cli-1", "Em preparacao"));

        verifyNoInteractions(push);
    }

    @Test
    void deveAvisarALojaDoPedidoNovo() {
        listener.onOrderPlaced(new OrderPlacedEvent("94c103db-8f0c"));

        verify(push).notifyUser(eq("loja-1"), eq("Novo pedido #94c103db"),
                org.mockito.ArgumentMatchers.matches("Maria · R\\$\\s?111,96"), eq("/minha-loja/pedidos"));
    }

    @Test
    void deveAvisarOEntregadorAtribuido() {
        listener.onDelivererAssigned(new DelivererAssignedEvent("94c103db-8f0c"));

        verify(push).notifyUser("ent-1", "Nova entrega #94c103db", "Da Gás Guará Centro. Toque para ver a rota.",
                "/entregas/94c103db-8f0c");
    }

    @Test
    void naoDeveQuebrarSeOEnvioFalhar() {
        doThrow(new RuntimeException("fora")).when(push).notifyUser(any(), any(), any(), any());

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                listener.onOrderPlaced(new OrderPlacedEvent("94c103db-8f0c")));
    }

    private static <T> T eq(T v) {
        return org.mockito.ArgumentMatchers.eq(v);
    }
}
