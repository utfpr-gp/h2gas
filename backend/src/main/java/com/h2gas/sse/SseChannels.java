package com.h2gas.sse;

/** HU08: convenção de canais SSE. Um canal por cliente, com o orderId no payload do evento. */
public final class SseChannels {

    static final String CUSTOMER_PREFIX = "customer:";

    private SseChannels() {
    }

    public static String customer(String customerId) {
        return CUSTOMER_PREFIX + customerId;
    }

    /** Só o próprio cliente assina o canal dele; canal fora da convenção nunca é permitido. */
    public static boolean isOwnCustomerChannel(String channel, String userId) {
        return userId != null && customer(userId).equals(channel);
    }
}
