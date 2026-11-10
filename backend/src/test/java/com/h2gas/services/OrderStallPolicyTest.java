package com.h2gas.services;

import com.h2gas.configuration.ClockConfig;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.model.entity.Order;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

// HU28 Cenário 7: pedido parado por etapa — pendente > 15 min, confirmado/em preparo > 1 h,
// em rota > 2 h, ou qualquer pedido em aberto de outro dia
class OrderStallPolicyTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 9, 15, 0);
    private final OrderStallPolicy policy = new OrderStallPolicy(
            Clock.fixed(AGORA.atZone(ClockConfig.BRASILIA).toInstant(), ClockConfig.BRASILIA));

    private Order pedido(OrderStatusEnum status, LocalDateTime criado, LocalDateTime atualizado, LocalDateTime despachado) {
        Order o = new Order();
        o.setStatus(status);
        o.setCreatedAt(criado);
        o.setUpdatedAt(atualizado);
        o.setDispatchedAt(despachado);
        return o;
    }

    private OptionalLong parado(Order o) {
        return policy.stalledMinutes(o);
    }

    @Test
    void deveMarcarPendenteDepoisDe15Minutos() {
        assertTrue(parado(pedido(OrderStatusEnum.PENDING, AGORA.minusMinutes(10), null, null)).isEmpty());
        assertEquals(20, parado(pedido(OrderStatusEnum.PENDING, AGORA.minusMinutes(20), null, null)).getAsLong());
    }

    @Test
    void deveMarcarConfirmadoOuEmPreparoDepoisDeUmaHora() {
        LocalDateTime criado = AGORA.minusHours(3);
        assertTrue(parado(pedido(OrderStatusEnum.CONFIRMED, criado, AGORA.minusMinutes(50), null)).isEmpty());
        assertEquals(70, parado(pedido(OrderStatusEnum.IN_PREPARATION, criado, AGORA.minusMinutes(70), null)).getAsLong());
    }

    @Test
    void deveMarcarEmRotaDepoisDeDuasHorasDoDespacho() {
        LocalDateTime criado = AGORA.minusHours(4);
        assertTrue(parado(pedido(OrderStatusEnum.OUT_FOR_DELIVERY, criado, AGORA.minusMinutes(5), AGORA.minusMinutes(90))).isEmpty());
        assertEquals(130, parado(pedido(OrderStatusEnum.OUT_FOR_DELIVERY, criado, AGORA.minusMinutes(5), AGORA.minusMinutes(130))).getAsLong());
    }

    @Test
    void deveMarcarPedidoEmAbertoDeOutroDia() {
        // confirmado há 10 min, mas feito ontem: está esquecido
        Order ontem = pedido(OrderStatusEnum.CONFIRMED, AGORA.minusDays(1).withHour(23), AGORA.minusMinutes(10), null);
        assertEquals(10, parado(ontem).getAsLong());
        Order seisDias = pedido(OrderStatusEnum.OUT_FOR_DELIVERY, AGORA.minusDays(6), AGORA.minusDays(6), AGORA.minusDays(6));
        assertEquals(6 * 24 * 60, parado(seisDias).getAsLong());
    }

    @Test
    void naoDeveMarcarPedidoEncerrado() {
        assertTrue(parado(pedido(OrderStatusEnum.DELIVERED, AGORA.minusDays(6), AGORA.minusDays(6), AGORA.minusDays(6))).isEmpty());
        assertTrue(parado(pedido(OrderStatusEnum.CANCELED, AGORA.minusDays(6), AGORA.minusDays(6), null)).isEmpty());
    }
}
