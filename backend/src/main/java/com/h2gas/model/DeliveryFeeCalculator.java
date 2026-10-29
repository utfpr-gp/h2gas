package com.h2gas.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

// HU17: frete por distância em linha reta (Haversine). Regra única, usada pela vitrine, pelo carrinho e pelo pedido (RN01).
// Sem coordenada de um dos lados: a entrega segue possível e o frete é só a taxa fixa (decisão do autor, 2026-10-28).
public final class DeliveryFeeCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private DeliveryFeeCalculator() {
    }

    public static BigDecimal distanceKm(Coordinates from, Coordinates to) {
        double lat1 = Math.toRadians(from.latitude());
        double lat2 = Math.toRadians(to.latitude());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(to.longitude() - from.longitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double km = EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(km).setScale(1, RoundingMode.HALF_UP);
    }

    public static DeliveryQuote quote(DeliveryTerms terms, Coordinates store, Coordinates destination) {
        if (!terms.offersDelivery()) {
            // só retirada: sem frete, mas a distância ajuda quem vai buscar
            return DeliveryQuote.unavailable(store == null || destination == null ? null : distanceKm(store, destination));
        }
        BigDecimal baseFee = orZero(terms.baseFee());
        if (store == null || destination == null) {
            return new DeliveryQuote(true, null, baseFee.setScale(2, RoundingMode.HALF_UP));
        }
        BigDecimal distance = distanceKm(store, destination);
        if (terms.radiusKm() != null && distance.compareTo(terms.radiusKm()) > 0) {
            return DeliveryQuote.unavailable(distance);
        }
        BigDecimal fee = baseFee.add(orZero(terms.feePerKm()).multiply(distance)).setScale(2, RoundingMode.HALF_UP);
        return new DeliveryQuote(true, distance, fee);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
