package com.h2gas.services;

// HU26: envio de mensagem de WhatsApp; a implementação real é o Twilio
public interface WhatsAppSender {

    // falso quando falta credencial ou ela ainda é o valor de exemplo
    boolean isConfigured();

    // lança exceção se não está configurado, se o número é inválido ou se o envio falha
    void send(String phone, String text);
}
