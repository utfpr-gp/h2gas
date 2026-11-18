package com.h2gas.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2gas.model.entity.PushSubscription;
import com.h2gas.model.repository.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

// HU27: notificação para todos os aparelhos do usuário; assinatura vencida (404/410) é apagada
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ICON = "/icons/icon-192x192.png";

    private final WebPushSender sender;
    private final PushSubscriptionRepository repository;

    public PushNotificationService(WebPushSender sender, PushSubscriptionRepository repository) {
        this.sender = sender;
        this.repository = repository;
    }

    public void notifyUser(String userId, String title, String body, String url) {
        if (!sender.isConfigured()) {
            return;
        }
        String payload = payload(title, body, url);
        for (PushSubscription subscription : repository.findByUserId(userId)) {
            try {
                int status = sender.send(subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth(), payload);
                if (status == 404 || status == 410) {
                    repository.delete(subscription);
                } else if (status >= 400) {
                    log.warn("Serviço de push respondeu {} para um aparelho do usuário {}", status, userId);
                }
            } catch (Exception e) {
                // um aparelho fora do ar não impede os outros; sem endpoint nem chaves no log
                log.warn("Não foi possível enviar push para um aparelho do usuário {}: {}", userId, e.getMessage());
            }
        }
    }

    // formato que o service worker do Angular (ngsw) mostra e abre ao tocar
    private String payload(String title, String body, String url) {
        Map<String, Object> notification = Map.of(
                "title", title,
                "body", body,
                "icon", ICON,
                "data", Map.of("onActionClick", Map.of("default", Map.of("operation", "navigateLastFocusedOrOpen", "url", url))));
        try {
            return JSON.writeValueAsString(Map.of("notification", notification));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
