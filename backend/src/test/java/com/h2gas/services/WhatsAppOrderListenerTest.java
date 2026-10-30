package com.h2gas.services;

import com.h2gas.enums.AppSettingKey;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.events.OrderStatusChangedEvent;
import com.h2gas.model.entity.Deliverer;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// HU26: aviso de saiu para entrega ao cliente, depois do commit
class WhatsAppOrderListenerTest {

    private final WhatsAppSender sender = mock(WhatsAppSender.class);
    private final AppSettingService settings = mock(AppSettingService.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final WhatsAppOrderListener listener = new WhatsAppOrderListener(sender, settings, orders);

    @BeforeEach
    void setUp() {
        Supplier loja = new Supplier();
        loja.setName("Gás Guará Centro");
        Deliverer entregador = new Deliverer();
        entregador.setUser(new User("e1", "João Entregador", "j@teste.com", "42900000000", null, "s", UserTypeEnum.DELIVERER));
        Order o = new Order();
        o.setId("94c103db-8f0c");
        o.setCustomer(new User("c1", "Maria", "m@teste.com", "42990000011", null, "s", UserTypeEnum.CUSTOMER));
        o.setSupplier(loja);
        o.setDeliverer(entregador);
        when(orders.findById("94c103db-8f0c")).thenReturn(Optional.of(o));
    }

    private void ligado(boolean ligado) {
        when(settings.isActive(AppSettingKey.WHATSAPP_ORDER_OUT_FOR_DELIVERY)).thenReturn(ligado);
    }

    @Test
    void deveAvisarQuandoSaiParaEntrega() {
        ligado(true);

        listener.onOrderStatusChanged(new OrderStatusChangedEvent("94c103db-8f0c", "c1", "Saiu para entrega"));

        verify(sender).send("42990000011",
                "Seu pedido #94c103db da Gás Guará Centro saiu para entrega com João. Acompanhe no H2Gás.");
    }

    @Test
    void naoDeveAvisarOutroStatus() {
        ligado(true);

        listener.onOrderStatusChanged(new OrderStatusChangedEvent("94c103db-8f0c", "c1", "Entregue"));

        verifyNoInteractions(sender);
        verify(orders, never()).findById(any());
    }

    @Test
    void naoDeveAvisarDesligado() {
        ligado(false);

        listener.onOrderStatusChanged(new OrderStatusChangedEvent("94c103db-8f0c", "c1", "Saiu para entrega"));

        verifyNoInteractions(sender);
    }

    @Test
    void naoDeveQuebrarSeOEnvioFalhar() {
        ligado(true);
        doThrow(new RuntimeException("Twilio fora")).when(sender).send(any(), any());

        assertDoesNotThrow(() -> listener.onOrderStatusChanged(
                new OrderStatusChangedEvent("94c103db-8f0c", "c1", "Saiu para entrega")));
    }
}
