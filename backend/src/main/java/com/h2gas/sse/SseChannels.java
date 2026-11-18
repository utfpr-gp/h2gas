package com.h2gas.sse;

/** HU08: convenção de canais SSE. Um canal por cliente, com o orderId no payload do evento. */
public final class SseChannels {

    static final String CUSTOMER_PREFIX = "customer:";
    static final String SUPPLIER_PREFIX = "supplier:";

    private SseChannels() {
    }

    public static String customer(String customerId) {
        return CUSTOMER_PREFIX + customerId;
    }

    /** HU27: canal da loja, com os pedidos novos e as mudanças de status. */
    public static String supplier(String supplierId) {
        return SUPPLIER_PREFIX + supplierId;
    }

    /** Só o próprio cliente assina o canal dele; canal fora da convenção nunca é permitido. */
    public static boolean isOwnCustomerChannel(String channel, String userId) {
        return userId != null && customer(userId).equals(channel);
    }

    /** HU27: cada usuário só assina o próprio canal, de cliente (customer:{id}) ou de loja (supplier:{id}). */
    public static boolean isOwnChannel(String channel, String userId) {
        return userId != null && (customer(userId).equals(channel) || supplier(userId).equals(channel));
    }
}
