package com.h2gas.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.h2gas.model.Coordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// HU17: latitude e longitude de um CEP, para o frete por km. AwesomeAPI primeiro, BrasilAPI v2 como alternativa;
// nenhuma exige chave. A BrasilAPI devolve o centro da cidade para muitos CEPs de Curitiba (achado no teste
// manual de 2026-10-28), o que zeraria as distâncias — por isso ela fica só de reserva. Falha, timeout ou CEP sem coordenada viram vazio (o frete cai para a taxa fixa), nunca exceção.
@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final int TIMEOUT_MS = 3000;

    private final RestClient restClient;
    private final String brasilApiUrl;
    private final String awesomeApiUrl;
    // só acertos entram no cache: uma API fora do ar agora não deve marcar o CEP como "sem coordenada" para sempre
    private final Map<String, Coordinates> cache = new ConcurrentHashMap<>();

    @Autowired
    public GeocodingService(RestClient.Builder builder,
                            @Value("${geocoding.brasilapi-url:https://brasilapi.com.br/api/cep/v2}") String brasilApiUrl,
                            @Value("${geocoding.awesomeapi-url:https://cep.awesomeapi.com.br/json}") String awesomeApiUrl) {
        this(builder.requestFactory(timeoutFactory()).build(), brasilApiUrl, awesomeApiUrl);
    }

    GeocodingService(RestClient restClient, String brasilApiUrl, String awesomeApiUrl) {
        this.restClient = restClient;
        this.brasilApiUrl = brasilApiUrl;
        this.awesomeApiUrl = awesomeApiUrl;
    }

    private static SimpleClientHttpRequestFactory timeoutFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        return factory;
    }

    public Optional<Coordinates> findByZipCode(String zipCode) {
        String cep = zipCode == null ? "" : zipCode.replaceAll("\\D", "");
        if (cep.length() != 8) {
            return Optional.empty();
        }
        Coordinates cached = cache.get(cep);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<Coordinates> found = fromAwesomeApi(cep).or(() -> fromBrasilApi(cep));
        found.ifPresentOrElse(c -> cache.put(cep, c), () -> log.warn("Sem coordenadas para o CEP {}", cep));
        return found;
    }

    private Optional<Coordinates> fromBrasilApi(String cep) {
        JsonNode coordinates = fetch(brasilApiUrl + "/" + cep).map(b -> b.path("location").path("coordinates")).orElse(null);
        return coordinates == null ? Optional.empty() : parse(coordinates.path("latitude"), coordinates.path("longitude"));
    }

    private Optional<Coordinates> fromAwesomeApi(String cep) {
        return fetch(awesomeApiUrl + "/" + cep).flatMap(b -> parse(b.path("lat"), b.path("lng")));
    }

    private Optional<JsonNode> fetch(String url) {
        try {
            return Optional.ofNullable(restClient.get().uri(url).retrieve().body(JsonNode.class));
        } catch (RuntimeException e) {
            log.warn("Consulta de CEP falhou em {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<Coordinates> parse(JsonNode lat, JsonNode lng) {
        try {
            if (lat.isMissingNode() || lng.isMissingNode() || lat.asText().isBlank() || lng.asText().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new Coordinates(Double.parseDouble(lat.asText()), Double.parseDouble(lng.asText())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
