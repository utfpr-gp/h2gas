package com.h2gas.sse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// HU27: além do canal do cliente, a loja tem o canal dela; cada um só do próprio usuário
class SseChannelsTest {

    @Test
    void deveAceitarOCanalDaPropriaLoja() {
        assertEquals("supplier:loja-1", SseChannels.supplier("loja-1"));
        assertTrue(SseChannels.isOwnChannel("supplier:loja-1", "loja-1"));
    }

    @Test
    void naoDeveAceitarCanalDeOutraLoja() {
        assertFalse(SseChannels.isOwnChannel("supplier:loja-2", "loja-1"));
        assertFalse(SseChannels.isOwnChannel("order:123", "loja-1"));
        assertFalse(SseChannels.isOwnChannel("supplier:loja-1", null));
    }

    @Test
    void deveManterOCanalDoCliente() {
        assertTrue(SseChannels.isOwnChannel("customer:cli-1", "cli-1"));
        assertFalse(SseChannels.isOwnChannel("customer:cli-2", "cli-1"));
    }
}
