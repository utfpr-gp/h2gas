package com.h2gas.events;

/**
 * HU08: o status de um pedido mudou. Publicado pelo OrderService dentro da transação; o envio SSE
 * ao cliente só acontece depois do commit (ver OrderStatusSseListener).
 *
 * @param supplierId loja do pedido (HU27: também recebe o evento); nula quando não importa
 * @param status rótulo do status, o mesmo do OrderResponseDTO (ex.: "Saiu para entrega")
 */
public record OrderStatusChangedEvent(String orderId, String customerId, String supplierId, String status) {

    /** Sem loja (usado onde só o cliente importa, como o push da HU27). */
    public OrderStatusChangedEvent(String orderId, String customerId, String status) {
        this(orderId, customerId, null, status);
    }
}
