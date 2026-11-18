package com.h2gas.model.dto;

// HU17: de onde medir (CEP do cliente ou do visitante) e os filtros de entrega da vitrine
// deliveryMode: DELIVERY = "Entregam aqui", PICKUP = "Retirada", nulo = todas; freeDelivery: só entrega grátis
public record StorefrontFilter(String zipCode, String deliveryMode, Boolean freeDelivery) {

    public static final StorefrontFilter NONE = new StorefrontFilter(null, null, null);
}
