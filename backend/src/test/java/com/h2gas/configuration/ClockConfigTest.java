package com.h2gas.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

// F12 item (b): o backend grava e compara datas no horário de Brasília, mesmo num servidor em UTC
class ClockConfigTest {

    private final TimeZone original = TimeZone.getDefault();

    @AfterEach
    void restaurarFuso() {
        TimeZone.setDefault(original);
    }

    @Test
    void deveUsarBrasiliaComoFusoPadrao() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); // como o container do Docker/Coolify

        ClockConfig.applyDefaultTimeZone();

        assertEquals("America/Sao_Paulo", TimeZone.getDefault().getID());
    }

    // F12: o fuso vem da variável APP_TIMEZONE; sem ela, Brasília
    @Test
    void deveLerOFusoDaVariavelDeAmbiente() {
        assertEquals("America/Sao_Paulo", ClockConfig.zoneFrom(null).getId());
        assertEquals("America/Sao_Paulo", ClockConfig.zoneFrom("  ").getId());
        assertEquals("America/Manaus", ClockConfig.zoneFrom("America/Manaus").getId());
        org.junit.jupiter.api.Assertions.assertThrows(java.time.DateTimeException.class,
                () -> ClockConfig.zoneFrom("Fuso/Inexistente"));
    }
}
