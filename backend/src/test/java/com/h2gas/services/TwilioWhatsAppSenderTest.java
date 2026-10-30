package com.h2gas.services;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// HU26: envio pela API de mensagens do Twilio, autenticado pela API Key
class TwilioWhatsAppSenderTest {

    private static final String URL = "https://api.twilio.com/2010-04-01/Accounts/AC123/Messages.json";

    private record Montado(TwilioWhatsAppSender sender, MockRestServiceServer server) {}

    private Montado montar(String account, String keySid, String secret, String from) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Montado(new TwilioWhatsAppSender(builder.build(), account, keySid, secret, from), server);
    }

    @Test
    void deveMandarFormularioComAutenticacao() {
        Montado m = montar("AC123", "SK456", "segredo", "whatsapp:+14155238886");
        String basic = "Basic " + Base64.getEncoder().encodeToString("SK456:segredo".getBytes(StandardCharsets.UTF_8));
        m.server().expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basic))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formDataContains(java.util.Map.of(
                        "From", "whatsapp:+14155238886",
                        "To", "whatsapp:+5542999990000",
                        "Body", "Olá")))
                .andRespond(withSuccess("{\"sid\":\"SM1\"}", MediaType.APPLICATION_JSON));

        m.sender().send("(42) 99999-0000", "Olá");

        m.server().verify();
    }

    @Test
    void deveMontarONumeroComDdi() {
        assertEquals("whatsapp:+5542999990000", TwilioWhatsAppSender.toWhatsApp("42999990000"));
        assertEquals("whatsapp:+5541999990000", TwilioWhatsAppSender.toWhatsApp("+55 41 99999-0000"));
        // DDD 55 (Santa Maria) com 11 dígitos ainda ganha o DDI
        assertEquals("whatsapp:+5555999990000", TwilioWhatsAppSender.toWhatsApp("55999990000"));
        assertNull(TwilioWhatsAppSender.toWhatsApp("9999-000"));
        assertNull(TwilioWhatsAppSender.toWhatsApp(null));
    }

    @Test
    void naoDeveEstarConfiguradoComPlaceholder() {
        assertTrue(montar("AC123", "SK456", "segredo", "whatsapp:+1415").sender().isConfigured());
        assertFalse(montar("KEY", "SK456", "segredo", "whatsapp:+1415").sender().isConfigured());
        assertFalse(montar("AC123", "", "segredo", "whatsapp:+1415").sender().isConfigured());
        assertFalse(montar("AC123", "SK456", "fake", "whatsapp:+1415").sender().isConfigured());
        assertFalse(montar("AC123", "SK456", "segredo", null).sender().isConfigured());
    }

    @Test
    void deveRecusarEnviarSemConfiguracaoOuNumero() {
        assertThrows(IllegalStateException.class, () -> montar("KEY", "KEY", "KEY", "KEY").sender().send("42999990000", "Oi"));
        assertThrows(IllegalArgumentException.class,
                () -> montar("AC123", "SK456", "segredo", "whatsapp:+1415").sender().send("123", "Oi"));
    }

    @Test
    void deveLancarQuandoOTwilioFalha() {
        Montado m = montar("AC123", "SK456", "segredo", "whatsapp:+14155238886");
        m.server().expect(requestTo(URL)).andRespond(withServerError());

        assertThrows(RuntimeException.class, () -> m.sender().send("42999990000", "Oi"));
    }
}
