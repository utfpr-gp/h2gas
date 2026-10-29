package com.h2gas.services;

import com.h2gas.model.Coordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// HU17: coordenadas do CEP pela AwesomeAPI; BrasilAPI como reserva (ela devolve o centro da cidade para muitos CEPs);
// falha vira vazio, nunca exceção
class GeocodingServiceTest {

    private static final String BRASIL = "https://brasil.test/api/cep/v2";
    private static final String AWESOME = "https://awesome.test/json";

    private MockRestServiceServer server;
    private GeocodingService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new GeocodingService(builder.build(), BRASIL, AWESOME);
    }

    @Test
    @DisplayName("HU17: coordenadas pelo CEP (AwesomeAPI)")
    void deveObterCoordenadasPeloCep() {
        server.expect(requestTo(AWESOME + "/80020310")).andRespond(withSuccess(
                "{\"cep\":\"80020310\",\"lat\":\"-25.4296\",\"lng\":\"-49.2713\"}", MediaType.APPLICATION_JSON));

        Optional<Coordinates> c = service.findByZipCode("80020-310");

        assertTrue(c.isPresent());
        assertEquals(-25.4296, c.get().latitude(), 1e-6);
        assertEquals(-49.2713, c.get().longitude(), 1e-6);
        server.verify();
    }

    @Test
    @DisplayName("HU17: AwesomeAPI fora do ar, usa a BrasilAPI")
    void deveUsarAFonteAlternativaQuandoAPrimeiraFalha() {
        server.expect(requestTo(AWESOME + "/80420090")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(BRASIL + "/80420090")).andRespond(withSuccess(
                "{\"cep\":\"80420090\",\"location\":{\"type\":\"Point\",\"coordinates\":{\"longitude\":\"-49.2867\",\"latitude\":\"-25.4459\"}}}",
                MediaType.APPLICATION_JSON));

        Optional<Coordinates> c = service.findByZipCode("80420090");

        assertEquals(-25.4459, c.orElseThrow().latitude(), 1e-6);
        server.verify();
    }

    @Test
    @DisplayName("HU17: as duas fontes falhando devolvem vazio, sem exceção")
    void naoDeveQuebrarQuandoAsDuasFalham() {
        server.expect(requestTo(AWESOME + "/80000000")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(BRASIL + "/80000000")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertTrue(service.findByZipCode("80000000").isEmpty());
        server.verify();
    }

    @Test
    @DisplayName("HU17: o mesmo CEP não é consultado duas vezes")
    void deveUsarOCacheNoMesmoCep() {
        server.expect(ExpectedCount.once(), requestTo(AWESOME + "/80020310")).andRespond(withSuccess(
                "{\"lat\":\"-25.4296\",\"lng\":\"-49.2713\"}", MediaType.APPLICATION_JSON));

        service.findByZipCode("80020-310");
        Optional<Coordinates> segunda = service.findByZipCode("80020310");

        assertTrue(segunda.isPresent());
        server.verify();
    }

    @Test
    @DisplayName("HU17: CEP inválido nem chega a consultar")
    void deveIgnorarCepInvalido() {
        assertTrue(service.findByZipCode("123").isEmpty());
        assertTrue(service.findByZipCode(null).isEmpty());
        server.verify();
    }
}
