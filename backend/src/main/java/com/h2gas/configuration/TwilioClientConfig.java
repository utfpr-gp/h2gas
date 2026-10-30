package com.h2gas.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// HU26: cliente HTTP do Twilio com timeout curto; uma mensagem lenta não pode segurar o login nem a rota
@Configuration
public class TwilioClientConfig {

    @Bean("twilioRestClient")
    public RestClient twilioRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        return RestClient.builder().requestFactory(factory).build();
    }
}
