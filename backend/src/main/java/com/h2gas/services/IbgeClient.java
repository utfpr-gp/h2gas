package com.h2gas.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

// F7 fatia 6: consulta a API pública de localidades do IBGE (sem chave)
@Component
public class IbgeClient {

    public record Municipality(int code, String name, String state) {
    }

    private final RestClient restClient;

    public IbgeClient(RestClient.Builder builder,
                      @Value("${ibge.api-url:https://servicodados.ibge.gov.br/api/v1/localidades}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    // código inexistente: o IBGE responde 200 com "[]"
    public Optional<Municipality> findMunicipality(int code) {
        JsonNode body;
        try {
            body = restClient.get().uri("/municipios/{code}", code).retrieve().body(JsonNode.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível consultar o IBGE agora. Tente de novo em instantes.", e);
        }
        if (body == null || !body.hasNonNull("nome")) {
            return Optional.empty();
        }
        // alguns municípios novos vêm sem microrregião; a região imediata sempre traz a UF
        JsonNode uf = body.path("regiao-imediata").path("regiao-intermediaria").path("UF");
        if (uf.isMissingNode()) {
            uf = body.path("microrregiao").path("mesorregiao").path("UF");
        }
        return Optional.of(new Municipality(body.path("id").asInt(), body.get("nome").asText(), uf.path("sigla").asText()));
    }
}
