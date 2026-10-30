package com.h2gas.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Stream;

// HU26: API de mensagens do Twilio, autenticada pela API Key (SK + secret); sem SDK
@Service
public class TwilioWhatsAppSender implements WhatsAppSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppSender.class);
    private static final Set<String> PLACEHOLDERS = Set.of("KEY", "FAKE");

    private final RestClient restClient;
    private final String accountSid;
    private final String apiKeySid;
    private final String apiKeySecret;
    private final String from;

    public TwilioWhatsAppSender(@Qualifier("twilioRestClient") RestClient restClient,
                                @Value("${twilio.account.sid:}") String accountSid,
                                @Value("${twilio.api-key.sid:}") String apiKeySid,
                                @Value("${twilio.api-key.secret:}") String apiKeySecret,
                                @Value("${twilio.whatsapp.from:}") String from) {
        this.restClient = restClient;
        this.accountSid = accountSid;
        this.apiKeySid = apiKeySid;
        this.apiKeySecret = apiKeySecret;
        this.from = from;
    }

    @Override
    public boolean isConfigured() {
        return Stream.of(accountSid, apiKeySid, apiKeySecret, from)
                .allMatch(v -> v != null && !v.isBlank() && !PLACEHOLDERS.contains(v.trim().toUpperCase()));
    }

    @Override
    public void send(String phone, String text) {
        if (!isConfigured()) {
            throw new IllegalStateException("Twilio não configurado.");
        }
        String to = toWhatsApp(phone);
        if (to == null) {
            throw new IllegalArgumentException("Telefone inválido para WhatsApp.");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("From", from);
        form.add("To", to);
        form.add("Body", text);
        String basic = Base64.getEncoder().encodeToString((apiKeySid + ":" + apiKeySecret).getBytes(StandardCharsets.UTF_8));
        restClient.post()
                .uri("https://api.twilio.com/2010-04-01/Accounts/{account}/Messages.json", accountSid)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
        // sem número completo nem texto no log (o texto pode ter o código de login)
        log.info("WhatsApp enviado para final {}", to.substring(to.length() - 4));
    }

    // mesma regra do whatsapp-link.ts (HU25): só considera que o 55 já está lá com mais de 11 dígitos
    static String toWhatsApp(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        if (digits.length() < 10) return null;
        return "whatsapp:+" + (digits.startsWith("55") && digits.length() > 11 ? digits : "55" + digits);
    }
}
