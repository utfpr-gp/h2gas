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
    @MockitoBean private PhoneMessageSender sender;

    @Test
    void deveNascerDesligado() {
        when(sender.isConfigured(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        AppSettingsDTO dto = appSettingService.list();

        assertTrue(dto.whatsappConfigured());
        assertTrue(dto.smsConfigured());
        assertEquals(AppSettingKey.values().length, dto.settings().size());
        dto.settings().forEach(s -> assertFalse(s.enabled(), s.key().name()));
        assertFalse(appSettingService.isEnabled(AppSettingKey.PHONE_LOGIN_CODE));
    }

    @Test
    void deveLigarEDesligar() {
        when(sender.isConfigured(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        appSettingService.update(AppSettingKey.PHONE_ORDER_OUT_FOR_DELIVERY, true);
        assertTrue(appSettingService.isEnabled(AppSettingKey.PHONE_ORDER_OUT_FOR_DELIVERY));
        assertFalse(appSettingService.isEnabled(AppSettingKey.PHONE_LOGIN_CODE));

        appSettingService.update(AppSettingKey.PHONE_ORDER_OUT_FOR_DELIVERY, false);
        assertFalse(appSettingService.isEnabled(AppSettingKey.PHONE_ORDER_OUT_FOR_DELIVERY));
    }

    @Test
    void deveRecusarLigarSemTwilio() {
        when(sender.isConfigured(org.mockito.ArgumentMatchers.any())).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> appSettingService.update(AppSettingKey.PHONE_LOGIN_CODE, true));
        appSettingService.update(AppSettingKey.PHONE_LOGIN_CODE, false); // desligar sempre pode
        assertFalse(appSettingService.isEnabled(AppSettingKey.PHONE_LOGIN_CODE));
    }

    @Test
    void naoDeveValerLigadoSeOTwilioSair() {
        when(sender.isConfigured(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        appSettingService.update(AppSettingKey.PHONE_LOGIN_CODE, true);

        when(sender.isConfigured(org.mockito.ArgumentMatchers.any())).thenReturn(false);

        assertFalse(appSettingService.isActive(AppSettingKey.PHONE_LOGIN_CODE));
        assertTrue(appSettingService.isEnabled(AppSettingKey.PHONE_LOGIN_CODE));
    }

    @Test
    void deveUsarOWhatsappPorPadraoEOSmsQuandoEscolhido() {
        when(sender.isConfigured(com.h2gas.enums.PhoneChannel.WHATSAPP)).thenReturn(false);
        when(sender.isConfigured(com.h2gas.enums.PhoneChannel.SMS)).thenReturn(true);
        assertEquals(com.h2gas.enums.PhoneChannel.WHATSAPP, appSettingService.phoneChannel());
        // no WhatsApp sem configuração não dá para ligar
        assertThrows(BusinessRuleException.class, () -> appSettingService.update(AppSettingKey.PHONE_LOGIN_CODE, true));

        appSettingService.update(AppSettingKey.PHONE_USE_SMS, true);
        appSettingService.update(AppSettingKey.PHONE_LOGIN_CODE, true);

        assertEquals(com.h2gas.enums.PhoneChannel.SMS, appSettingService.phoneChannel());
        assertTrue(appSettingService.isActive(AppSettingKey.PHONE_LOGIN_CODE));
        // voltar para o WhatsApp sem ele configurado é recusado
        assertThrows(BusinessRuleException.class, () -> appSettingService.update(AppSettingKey.PHONE_USE_SMS, false));
    }
}
