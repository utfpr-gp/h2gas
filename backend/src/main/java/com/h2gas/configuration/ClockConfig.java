package com.h2gas.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

// HU18: "aberta agora" usa sempre a hora de Brasília, qualquer que seja o fuso do servidor
@Configuration
public class ClockConfig {

    public static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock clock() {
        return Clock.system(BRASILIA);
    }
}
