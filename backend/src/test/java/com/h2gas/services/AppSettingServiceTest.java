package com.h2gas.services;

import com.h2gas.enums.AppSettingKey;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.dto.AppSettingsDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

// HU26: interruptores do admin para as mensagens de WhatsApp
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppSettingServiceTest {

    @Autowired private AppSettingService appSettingService;
    @MockitoBean private WhatsAppSender whatsAppSender;

    @Test
    void deveNascerDesligado() {
        when(whatsAppSender.isConfigured()).thenReturn(true);

        AppSettingsDTO dto = appSettingService.list();

        assertTrue(dto.whatsappConfigured());
        assertEquals(AppSettingKey.values().length, dto.settings().size());
        dto.settings().forEach(s -> assertFalse(s.enabled(), s.key().name()));
        assertFalse(appSettingService.isEnabled(AppSettingKey.WHATSAPP_LOGIN_CODE));
    }

    @Test
    void deveLigarEDesligar() {
        when(whatsAppSender.isConfigured()).thenReturn(true);

        appSettingService.update(AppSettingKey.WHATSAPP_ORDER_OUT_FOR_DELIVERY, true);
        assertTrue(appSettingService.isEnabled(AppSettingKey.WHATSAPP_ORDER_OUT_FOR_DELIVERY));
        assertFalse(appSettingService.isEnabled(AppSettingKey.WHATSAPP_LOGIN_CODE));

        appSettingService.update(AppSettingKey.WHATSAPP_ORDER_OUT_FOR_DELIVERY, false);
        assertFalse(appSettingService.isEnabled(AppSettingKey.WHATSAPP_ORDER_OUT_FOR_DELIVERY));
    }

    @Test
    void deveRecusarLigarSemTwilio() {
        when(whatsAppSender.isConfigured()).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> appSettingService.update(AppSettingKey.WHATSAPP_LOGIN_CODE, true));
        appSettingService.update(AppSettingKey.WHATSAPP_LOGIN_CODE, false); // desligar sempre pode
        assertFalse(appSettingService.isEnabled(AppSettingKey.WHATSAPP_LOGIN_CODE));
    }

    @Test
    void naoDeveValerLigadoSeOTwilioSair() {
        when(whatsAppSender.isConfigured()).thenReturn(true);
        appSettingService.update(AppSettingKey.WHATSAPP_LOGIN_CODE, true);

        when(whatsAppSender.isConfigured()).thenReturn(false);

        assertFalse(appSettingService.isActive(AppSettingKey.WHATSAPP_LOGIN_CODE));
        assertTrue(appSettingService.isEnabled(AppSettingKey.WHATSAPP_LOGIN_CODE));
    }
}
