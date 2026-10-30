package com.h2gas.services;

import com.h2gas.enums.AppSettingKey;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.dto.AppSettingDTO;
import com.h2gas.model.dto.AppSettingsDTO;
import com.h2gas.model.entity.AppSetting;
import com.h2gas.model.repository.AppSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

// HU26: interruptores do admin; chave sem linha no banco conta como desligada
@Service
public class AppSettingService {

    private final AppSettingRepository repository;
    private final WhatsAppSender whatsAppSender;

    public AppSettingService(AppSettingRepository repository, WhatsAppSender whatsAppSender) {
        this.repository = repository;
        this.whatsAppSender = whatsAppSender;
    }

    // o que o admin escolheu
    @Transactional(readOnly = true)
    public boolean isEnabled(AppSettingKey key) {
        return repository.findById(key).map(AppSetting::isEnabled).orElse(false);
    }

    // o que vale de fato: ligado e com o Twilio configurado
    @Transactional(readOnly = true)
    public boolean isActive(AppSettingKey key) {
        return isEnabled(key) && whatsAppSender.isConfigured();
    }

    @Transactional(readOnly = true)
    public AppSettingsDTO list() {
        return new AppSettingsDTO(whatsAppSender.isConfigured(),
                Arrays.stream(AppSettingKey.values()).map(k -> new AppSettingDTO(k, isEnabled(k))).toList());
    }

    @Transactional
    public AppSettingDTO update(AppSettingKey key, boolean enabled) {
        if (enabled && !whatsAppSender.isConfigured()) {
            throw new BusinessRuleException("Twilio não configurado: preencha as variáveis do WhatsApp antes de ligar.");
        }
        AppSetting setting = repository.findById(key).orElseGet(() -> new AppSetting(key, false));
        setting.setEnabled(enabled);
        repository.save(setting);
        return new AppSettingDTO(key, enabled);
    }
}
