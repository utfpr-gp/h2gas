package com.h2gas.services;

import com.h2gas.enums.PhoneChannel;

// HU26: mensagem ao celular por WhatsApp ou SMS; a implementação real é o Twilio
public interface PhoneMessageSender {

    // falso quando falta credencial ou remetente do canal, ou eles ainda são o valor de exemplo
    boolean isConfigured(PhoneChannel channel);

    // lança exceção se o canal não está configurado, se o número é inválido ou se o envio falha
    void send(PhoneChannel channel, String phone, String text);
}
