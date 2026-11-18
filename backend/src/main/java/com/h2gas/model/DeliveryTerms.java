package com.h2gas.model;

import java.math.BigDecimal;

// HU17: como a loja entrega — raio nulo = sem limite
public record DeliveryTerms(boolean offersDelivery, BigDecimal radiusKm, BigDecimal baseFee, BigDecimal feePerKm) {

    public boolean freeDelivery() {
        return offersDelivery && isZero(baseFee) && isZero(feePerKm);
    }

    private static boolean isZero(BigDecimal value) {
        return value == null || value.signum() == 0;
    }
}
