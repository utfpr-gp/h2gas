package com.h2gas.model;

import java.math.BigDecimal;

// HU17: resultado do cálculo — distanceKm nulo = sem coordenada; fee nulo = não entrega ali
public record DeliveryQuote(boolean available, BigDecimal distanceKm, BigDecimal fee) {

    public static DeliveryQuote unavailable(BigDecimal distanceKm) {
        return new DeliveryQuote(false, distanceKm, null);
    }
}
