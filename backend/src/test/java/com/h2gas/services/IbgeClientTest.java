package com.h2gas.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IbgeClientTest {

    private static final String BASE = "https://ibge.test/api/v1/localidades";

    private MockRestServiceServer server;
    private IbgeClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new IbgeClient(builder, BASE);
    }

    @Test
    void deveLerNomeEUfDoMunicipio() {
        server.expect(requestTo(BASE + "/municipios/4106902")).andRespond(withSuccess("""
                {"id":4106902,"nome":"Curitiba","microrregiao":null,
                 "regiao-imediata":{"id":410001,"nome":"Curitiba","regiao-intermediaria":{"id":4101,"nome":"Curitiba",
                 "UF":{"id":41,"sigla":"PR","nome":"Paraná"}}}}
                """, MediaType.APPLICATION_JSON));

        Optional<IbgeClient.Municipality> result = client.findMunicipality(4106902);

        assertTrue(result.isPresent());
        assertEquals(4106902, result.get().code());
        assertEquals("Curitiba", result.get().name());
        assertEquals("PR", result.get().state());
    }

    @Test
    void deveDevolverVazioParaCodigoInexistente() {
        server.expect(requestTo(BASE + "/municipios/9999999")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.findMunicipality(9999999).isEmpty());
    }

    @Test
    void deveAvisarQuandoOIbgeNaoResponde() {
        server.expect(requestTo(BASE + "/municipios/4106902")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> client.findMunicipality(4106902));

        assertEquals(HttpStatus.BAD_GATEWAY, erro.getStatusCode());
        assertEquals("Não foi possível consultar o IBGE agora. Tente de novo em instantes.", erro.getReason());
    }
}
