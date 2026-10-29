package com.h2gas.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

// HU17: frete = taxa fixa + valor por km × distância em linha reta (Haversine)
class DeliveryFeeCalculatorTest {

    // Praça Tiradentes (centro de Curitiba) e Praça do Japão (Batel): ~2,4 km em linha reta
    private static final Coordinates CENTRO = new Coordinates(-25.4296, -49.2713);
    private static final Coordinates BATEL = new Coordinates(-25.4459, -49.2867);

    private static DeliveryTerms termos(boolean entrega, String raio, String fixa, String porKm) {
        return new DeliveryTerms(entrega, raio == null ? null : new BigDecimal(raio), new BigDecimal(fixa), new BigDecimal(porKm));
    }

    @Test
    @DisplayName("HU17: distância em linha reta entre dois pontos conhecidos")
    void deveCalcularADistanciaEntreDoisPontos() {
        BigDecimal km = DeliveryFeeCalculator.distanceKm(CENTRO, BATEL);

        assertEquals(2.4, km.doubleValue(), 0.2);
        assertEquals(1, km.scale());
    }

    @Test
    @DisplayName("HU17: soma a taxa fixa e o valor por km")
    void deveSomarTaxaFixaEValorPorKm() {
        DeliveryQuote q = DeliveryFeeCalculator.quote(termos(true, null, "5.00", "2.00"), CENTRO, BATEL);

        assertTrue(q.available());
        assertEquals(0, q.distanceKm().compareTo(DeliveryFeeCalculator.distanceKm(CENTRO, BATEL)));
        assertEquals(new BigDecimal("5.00").add(new BigDecimal("2.00").multiply(q.distanceKm())).setScale(2), q.fee());
    }

    @Test
    @DisplayName("HU17: frete arredondado a 2 casas (HALF_UP)")
    void deveArredondarParaDuasCasas() {
        DeliveryQuote q = DeliveryFeeCalculator.quote(termos(true, null, "0", "1.333"), CENTRO, BATEL);

        assertEquals(2, q.fee().scale());
    }

    @Test
    @DisplayName("HU17: fora do raio a loja não entrega")
    void naoDeveEntregarForaDoRaio() {
        DeliveryQuote q = DeliveryFeeCalculator.quote(termos(true, "1.0", "5.00", "2.00"), CENTRO, BATEL);

        assertFalse(q.available());
        assertNull(q.fee());
        assertNotNull(q.distanceKm());
    }

    @Test
    @DisplayName("HU17: raio nulo é sem limite")
    void deveTratarRaioNuloComoSemLimite() {
        Coordinates longe = new Coordinates(-23.5505, -46.6333); // São Paulo

        assertTrue(DeliveryFeeCalculator.quote(termos(true, null, "5.00", "0"), CENTRO, longe).available());
    }

    @Test
    @DisplayName("HU17: sem coordenada, cobra só a taxa fixa e não checa o raio")
    void deveCobrarSoATaxaFixaSemCoordenada() {
        DeliveryQuote q = DeliveryFeeCalculator.quote(termos(true, "1.0", "5.00", "2.00"), null, BATEL);

        assertTrue(q.available());
        assertNull(q.distanceKm());
        assertEquals(new BigDecimal("5.00"), q.fee());
    }

    @Test
    @DisplayName("HU17: loja que não faz entrega não entrega em lugar nenhum")
    void naoDeveEntregarQuandoALojaSoFazRetirada() {
        DeliveryQuote q = DeliveryFeeCalculator.quote(termos(false, null, "0", "0"), CENTRO, BATEL);

        assertFalse(q.available());
        assertNull(q.fee());
    }

    @Test
    @DisplayName("HU17: taxa fixa e por km zeradas = entrega grátis")
    void deveReconhecerEntregaGratis() {
        assertTrue(termos(true, null, "0", "0.00").freeDelivery());
        assertFalse(termos(true, null, "0", "0.50").freeDelivery());
        assertFalse(termos(false, null, "0", "0").freeDelivery());
    }
}
