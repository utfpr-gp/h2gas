package com.h2gas.services;

import com.h2gas.enums.PhoneChannel;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// HU26: envio pela API de mensagens do Twilio, autenticado pela API Key, por WhatsApp ou SMS
class TwilioPhoneMessageSenderTest {

    private static final String URL = "https://api.twilio.com/2010-04-01/Accounts/AC123/Messages.json";

    private record Montado(TwilioPhoneMessageSender sender, MockRestServiceServer server) {}

    private Montado montar(String account, String keySid, String secret, String whatsappFrom, String smsFrom) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Montado(new TwilioPhoneMessageSender(builder.build(), account, keySid, secret, whatsappFrom, smsFrom), server);
    }

    private Montado configurado() {
        return montar("AC123", "SK456", "segredo", "whatsapp:+14155238886", "+17372508034");
    }

    @Test
    void deveMandarWhatsappComAutenticacao() {
        Montado m = configurado();
        String basic = "Basic " + Base64.getEncoder().encodeToString("SK456:segredo".getBytes(StandardCharsets.UTF_8));
        m.server().expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basic))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formDataContains(Map.of(
                        "From", "whatsapp:+14155238886", "To", "whatsapp:+5542999990000", "Body", "Olá")))
                .andRespond(withSuccess("{\"sid\":\"SM1\"}", MediaType.APPLICATION_JSON));

        m.sender().send(PhoneChannel.WHATSAPP, "(42) 99999-0000", "Olá");

        m.server().verify();
    }

    @Test
    void deveMandarSmsSemOPrefixoDoWhatsapp() {
        Montado m = configurado();
        m.server().expect(requestTo(URL))
                .andExpect(content().formDataContains(Map.of("From", "+17372508034", "To", "+5542991583814", "Body", "Oi")))
                .andRespond(withSuccess("{\"sid\":\"SM2\"}", MediaType.APPLICATION_JSON));

        m.sender().send(PhoneChannel.SMS, "42991583814", "Oi");

        m.server().verify();
    }

    @Test
    void deveMontarONumeroComDdi() {
        assertEquals("+5542999990000", TwilioPhoneMessageSender.toE164("42999990000"));
        assertEquals("+5541999990000", TwilioPhoneMessageSender.toE164("+55 41 99999-0000"));
        // DDD 55 (Santa Maria) com 11 dígitos ainda ganha o DDI
        assertEquals("+5555999990000", TwilioPhoneMessageSender.toE164("55999990000"));
        assertNull(TwilioPhoneMessageSender.toE164("9999-000"));
        assertNull(TwilioPhoneMessageSender.toE164(null));
    }

    @Test
    void deveDizerQualCanalEstaConfigurado() {
        assertTrue(configurado().sender().isConfigured(PhoneChannel.WHATSAPP));
        assertTrue(configurado().sender().isConfigured(PhoneChannel.SMS));
        Montado soWhatsapp = montar("AC123", "SK456", "segredo", "whatsapp:+1415", "");
        assertTrue(soWhatsapp.sender().isConfigured(PhoneChannel.WHATSAPP));
        assertFalse(soWhatsapp.sender().isConfigured(PhoneChannel.SMS));
        assertFalse(montar("KEY", "SK456", "segredo", "whatsapp:+1415", "+1737").sender().isConfigured(PhoneChannel.SMS));
        assertFalse(montar("AC123", "SK456", "fake", "whatsapp:+1415", "+1737").sender().isConfigured(PhoneChannel.WHATSAPP));
    }

    @Test
    void deveRecusarEnviarSemConfiguracaoOuNumero() {
        assertThrows(IllegalStateException.class,
                () -> montar("KEY", "KEY", "KEY", "KEY", "KEY").sender().send(PhoneChannel.SMS, "42999990000", "Oi"));
        assertThrows(IllegalArgumentException.class, () -> configurado().sender().send(PhoneChannel.SMS, "123", "Oi"));
    }

    @Test
    void deveLancarQuandoOTwilioFalha() {
        Montado m = configurado();
        m.server().expect(requestTo(URL)).andRespond(withServerError());

        assertThrows(RuntimeException.class, () -> m.sender().send(PhoneChannel.SMS, "42999990000", "Oi"));
    }
}
