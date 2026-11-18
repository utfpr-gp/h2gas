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

    @Test
    @DisplayName("F7 fatia 6: arquivo acima do limite de upload vira 400 com a regra da imagem")
    void deveResponder400QuandoArquivoPassaDoLimite() {
        ResponseEntity<ErrorResponse> resposta = handler.handleMaxUploadSize(
                new org.springframework.web.multipart.MaxUploadSizeExceededException(1_048_576), request);

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals("Envie uma imagem JPG, PNG ou WEBP de até 1 MB.", resposta.getBody().getMessage());
    }

    @Test
    @DisplayName("F7 §5.26: banner acima do limite de upload vira 400 com a regra do banner (2 MB)")
    void deveResponder400ComARegraDoBannerQuandoPassaDoLimite() {
        ResponseEntity<ErrorResponse> resposta = handler.handleMaxUploadSize(
                new org.springframework.web.multipart.MaxUploadSizeExceededException(2_097_152),
                new MockHttpServletRequest("PUT", "/h2gas/api/suppliers/mine/banner"));

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals("Envie uma imagem JPG, PNG ou WEBP de até 2 MB.", resposta.getBody().getMessage());
    }
}
