package com.h2gas.services;

import com.h2gas.enums.PhoneChannel;
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

// HU26: API de mensagens do Twilio (WhatsApp ou SMS), autenticada pela API Key (SK + secret); sem SDK
@Service
public class TwilioPhoneMessageSender implements PhoneMessageSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioPhoneMessageSender.class);
    private static final Set<String> PLACEHOLDERS = Set.of("KEY", "FAKE");

    private final RestClient restClient;
    private final String accountSid;
    private final String apiKeySid;
    private final String apiKeySecret;
    private final String whatsappFrom;
    private final String smsFrom;

    public TwilioPhoneMessageSender(@Qualifier("twilioRestClient") RestClient restClient,
                                @Value("${twilio.account.sid:}") String accountSid,
                                @Value("${twilio.api-key.sid:}") String apiKeySid,
                                @Value("${twilio.api-key.secret:}") String apiKeySecret,
                                @Value("${twilio.whatsapp.from:}") String whatsappFrom,
                                @Value("${twilio.sms.from:}") String smsFrom) {
        this.restClient = restClient;
        this.accountSid = accountSid;
        this.apiKeySid = apiKeySid;
        this.apiKeySecret = apiKeySecret;
        this.whatsappFrom = whatsappFrom;
        this.smsFrom = smsFrom;
    }

    @Override
    public boolean isConfigured(PhoneChannel channel) {
        return Stream.of(accountSid, apiKeySid, apiKeySecret, fromOf(channel))
                .allMatch(v -> v != null && !v.isBlank() && !PLACEHOLDERS.contains(v.trim().toUpperCase()));
    }

    @Override
    public void send(PhoneChannel channel, String phone, String text) {
        if (!isConfigured(channel)) {
            throw new IllegalStateException("Twilio não configurado para " + channel + ".");
        }
        String number = toE164(phone);
        if (number == null) {
            throw new IllegalArgumentException("Telefone inválido.");
        }
        String to = channel == PhoneChannel.WHATSAPP ? "whatsapp:" + number : number;
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("From", fromOf(channel));
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
        log.info("{} enviado para final {}", channel, number.substring(number.length() - 4));
    }

    private String fromOf(PhoneChannel channel) {
        return channel == PhoneChannel.WHATSAPP ? whatsappFrom : smsFrom;
    }

    // mesma regra do whatsapp-link.ts (HU25): só considera que o 55 já está lá com mais de 11 dígitos
    static String toE164(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        if (digits.length() < 10) return null;
        return "+" + (digits.startsWith("55") && digits.length() > 11 ? digits : "55" + digits);
    }
}
