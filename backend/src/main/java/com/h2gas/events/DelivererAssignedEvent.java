package com.h2gas.events;

/**
 * HU27: a loja atribuiu o pedido a um entregador; ele é avisado por push depois do commit (PushOrderListener).
 */
public record DelivererAssignedEvent(String orderId) {
}
