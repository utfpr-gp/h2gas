package com.h2gas.events;

/**
 * HU08: o status de um pedido mudou. Publicado pelo OrderService dentro da transação; o envio SSE
 * ao cliente só acontece depois do commit (ver OrderStatusSseListener).
 *
 * @param status rótulo do status, o mesmo do OrderResponseDTO (ex.: "Saiu para entrega")
 */
public record OrderStatusChangedEvent(String orderId, String customerId, String status) {
}
