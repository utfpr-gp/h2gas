package com.h2gas.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

// HU18: "aberta agora" usa sempre a hora de Brasília, qualquer que seja o fuso do servidor
@Configuration
public class ClockConfig {

    public static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    // F12: fuso da aplicação, lido da variável APP_TIMEZONE; sem ela, Brasília. Fuso inválido derruba a
    // subida (DateTimeException) em vez de cair silenciosamente em UTC.
    public static ZoneId zoneFrom(String value) {
        return value == null || value.isBlank() ? BRASILIA : ZoneId.of(value.trim());
    }

    // F12: o servidor roda em UTC. O Java passa a trabalhar no fuso da aplicação (LocalDateTime.now(),
    // @CreationTimestamp, "hoje", mês do painel) e o driver do MySQL grava em UTC
    // (connectionTimeZone=UTC no application.properties). Chamado no main, antes de abrir conexões.
    public static void applyDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneFrom(System.getenv("APP_TIMEZONE"))));
    }

    @Bean
    public Clock clock() {
        return Clock.system(BRASILIA);
    }
}
