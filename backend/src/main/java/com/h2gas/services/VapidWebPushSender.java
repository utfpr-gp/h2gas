package com.h2gas.services;

import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Security;
import java.util.stream.Stream;

// HU27: Web Push assinado com VAPID (biblioteca web-push); sem chaves, nada é enviado
@Service
public class VapidWebPushSender implements WebPushSender {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private PushService pushService;

    public VapidWebPushSender(@Value("${vapid.public-key:}") String publicKey,
                              @Value("${vapid.private-key:}") String privateKey,
                              @Value("${vapid.subject:}") String subject) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
    }

    @Override
    public boolean isConfigured() {
        return Stream.of(publicKey, privateKey, subject).allMatch(v -> v != null && !v.isBlank() && !v.equalsIgnoreCase("KEY"));
    }

    @Override
    public String publicKey() {
        return isConfigured() ? publicKey : "";
    }

    @Override
    public int send(String endpoint, String p256dh, String auth, String payloadJson) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Push não configurado.");
        }
        Notification notification = new Notification(endpoint, p256dh, auth, payloadJson);
        return service().send(notification, Encoding.AES128GCM).getStatusLine().getStatusCode();
    }

    private synchronized PushService service() throws Exception {
        if (pushService == null) {
            pushService = new PushService(publicKey, privateKey, subject);
        }
        return pushService;
    }
}
