package com.h2gas.services;

import com.h2gas.enums.AppSettingKey;
import com.h2gas.enums.PhoneChannel;
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
    private final PhoneMessageSender sender;

    public AppSettingService(AppSettingRepository repository, PhoneMessageSender sender) {
        this.repository = repository;
        this.sender = sender;
    }

    // o que o admin escolheu
    @Transactional(readOnly = true)
    public boolean isEnabled(AppSettingKey key) {
        return repository.findById(key).map(AppSetting::isEnabled).orElse(false);
    }

    @Transactional(readOnly = true)
    public PhoneChannel phoneChannel() {
        return isEnabled(AppSettingKey.PHONE_USE_SMS) ? PhoneChannel.SMS : PhoneChannel.WHATSAPP;
    }

    // o que vale de fato: ligado e com o canal escolhido configurado
    @Transactional(readOnly = true)
    public boolean isActive(AppSettingKey key) {
        return isEnabled(key) && sender.isConfigured(phoneChannel());
    }

    @Transactional(readOnly = true)
    public AppSettingsDTO list() {
        return new AppSettingsDTO(sender.isConfigured(PhoneChannel.WHATSAPP), sender.isConfigured(PhoneChannel.SMS),
                Arrays.stream(AppSettingKey.values()).map(k -> new AppSettingDTO(k, isEnabled(k))).toList());
    }

    @Transactional
    public AppSettingDTO update(AppSettingKey key, boolean enabled) {
        PhoneChannel needed = key == AppSettingKey.PHONE_USE_SMS
                ? (enabled ? PhoneChannel.SMS : PhoneChannel.WHATSAPP)
                : phoneChannel();
        // desligar uma mensagem sempre pode; trocar de canal ou ligar exige o canal configurado
        boolean switchesOn = enabled || key == AppSettingKey.PHONE_USE_SMS;
        if (switchesOn && !sender.isConfigured(needed)) {
            throw new BusinessRuleException("Twilio não configurado para " + (needed == PhoneChannel.SMS ? "SMS" : "WhatsApp")
                    + ": preencha as variáveis antes de ligar.");
        }
        AppSetting setting = repository.findById(key).orElseGet(() -> new AppSetting(key, false));
        setting.setEnabled(enabled);
        repository.save(setting);
        return new AppSettingDTO(key, enabled);
    }
}
