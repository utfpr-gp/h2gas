package com.h2gas.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;

// F7 §5.10: o usuário nunca vê texto técnico (Java/SQL) num erro interno
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/h2gas/api/orders");

    @Test
    @DisplayName("F7: erro inesperado vira 500 com mensagem amigável, sem o detalhe técnico")
    void deveEsconderDetalheTecnicoNoErroInterno() {
        ResponseEntity<ErrorResponse> resposta = handler.handleGeneric(
                new IllegalStateException("could not execute statement; SQL [insert into orders ...]"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resposta.getStatusCode());
        assertEquals("Erro interno. Tente novamente em instantes.", resposta.getBody().getMessage());
    }

    @Test
    @DisplayName("F7: ResponseStatusException mantém o status e o motivo dela")
    void deveUsarStatusEMotivoDaResponseStatusException() {
        ResponseEntity<ErrorResponse> resposta = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível enviar a imagem. Tente novamente."), request);

        assertEquals(HttpStatus.BAD_GATEWAY, resposta.getStatusCode());
        assertEquals("Não foi possível enviar a imagem. Tente novamente.", resposta.getBody().getMessage());
    }
}
