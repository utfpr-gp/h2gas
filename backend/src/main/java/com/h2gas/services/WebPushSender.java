package com.h2gas.services;

// HU27: envio de Web Push; a implementação real assina com VAPID
public interface WebPushSender {

    boolean isConfigured();

    String publicKey();

    // devolve o status HTTP do serviço de push do navegador (201 = aceito; 404/410 = assinatura vencida)
    int send(String endpoint, String p256dh, String auth, String payloadJson) throws Exception;
}
