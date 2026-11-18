package com.h2gas.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// HU18: a loja está aberta só dentro do horário de funcionamento
class OpeningHoursTest {

    private static final LocalTime OITO = LocalTime.of(8, 0);
    private static final LocalTime DEZOITO = LocalTime.of(18, 0);

    @Test
    @DisplayName("HU18: aberta dentro do horário")
    void deveEstarAbertaDentroDoHorario() {
        assertTrue(OpeningHours.isOpenAt(OITO, DEZOITO, LocalTime.of(10, 0)));
    }

    @Test
    @DisplayName("HU18: fechada fora do horário")
    void deveEstarFechadaForaDoHorario() {
        assertFalse(OpeningHours.isOpenAt(OITO, DEZOITO, LocalTime.of(19, 0)));
        assertFalse(OpeningHours.isOpenAt(OITO, DEZOITO, LocalTime.of(7, 59)));
    }

    @Test
    @DisplayName("HU18: a abertura conta, o fechamento não")
    void deveAbrirNaHoraDaAberturaEFecharNaHoraDoFechamento() {
        assertTrue(OpeningHours.isOpenAt(OITO, DEZOITO, OITO));
        assertFalse(OpeningHours.isOpenAt(OITO, DEZOITO, DEZOITO));
    }

    @Test
    @DisplayName("HU18: horário que vira a meia-noite (18:00–02:00)")
    void deveTratarHorarioQueViraAMeiaNoite() {
        LocalTime duas = LocalTime.of(2, 0);
        assertTrue(OpeningHours.isOpenAt(DEZOITO, duas, LocalTime.of(23, 0)));
        assertTrue(OpeningHours.isOpenAt(DEZOITO, duas, LocalTime.of(1, 0)));
        assertFalse(OpeningHours.isOpenAt(DEZOITO, duas, LocalTime.of(10, 0)));
        assertFalse(OpeningHours.isOpenAt(DEZOITO, duas, duas));
    }

    @Test
    @DisplayName("HU18: loja sem horário cadastrado é fechada")
    void deveTratarLojaSemHorarioComoFechada() {
        assertFalse(OpeningHours.isOpenAt(null, DEZOITO, LocalTime.of(10, 0)));
        assertFalse(OpeningHours.isOpenAt(OITO, null, LocalTime.of(10, 0)));
        assertFalse(OpeningHours.isOpenAt(OITO, OITO, LocalTime.of(10, 0)));
    }
}
