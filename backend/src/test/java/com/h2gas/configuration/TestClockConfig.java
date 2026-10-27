package com.h2gas.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.LocalDateTime;

// HU18: relógio fixo às 10:00 de Brasília, para "aberta agora" não depender da hora em que a suíte roda.
// Loja de teste aberta = 08:00–18:00; fechada = 19:00–23:00.
@Configuration
@Profile("test")
public class TestClockConfig {

    public static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 27, 10, 0);

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(AGORA.atZone(ClockConfig.BRASILIA).toInstant(), ClockConfig.BRASILIA);
    }
}
