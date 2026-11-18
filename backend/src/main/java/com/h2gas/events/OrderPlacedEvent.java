package com.h2gas.events;

/**
 * HU27: um pedido novo foi gravado; a loja é avisada por push depois do commit (PushOrderListener).
 */
public record OrderPlacedEvent(String orderId) {
}
