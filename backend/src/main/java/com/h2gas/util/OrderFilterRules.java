package com.h2gas.util;

import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.exceptions.BusinessRuleException;

import java.util.Locale;
import java.util.Set;

// F7 fatia 6: formas de pagamento e modalidades aceitas nos filtros de pedidos (histórico e dashboard)
public final class OrderFilterRules {
    private static final Set<String> PAYMENTS = Set.of("PIX", "DINHEIRO", "CREDIT_CARD");

    private OrderFilterRules() {
    }

    public static String parsePayment(String payment) {
        if (payment == null || payment.isBlank()) return null;
        String value = payment.trim().toUpperCase(Locale.ROOT);
        if (!PAYMENTS.contains(value)) {
            throw new BusinessRuleException("Forma de pagamento inválida: use PIX, DINHEIRO ou CREDIT_CARD.");
        }
        return value;
    }

    public static DeliveryModeEnum parseMode(String mode) {
        if (mode == null || mode.isBlank()) return null;
        try {
            return DeliveryModeEnum.valueOf(mode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Modalidade inválida: use DELIVERY ou PICKUP.");
        }
    }
}
