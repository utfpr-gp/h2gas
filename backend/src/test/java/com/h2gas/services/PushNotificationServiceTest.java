package com.h2gas.services;

import com.h2gas.model.entity.PushSubscription;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.PushSubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// HU27: manda a notificação para todos os aparelhos do usuário
class PushNotificationServiceTest {

    private final WebPushSender sender = mock(WebPushSender.class);
    private final PushSubscriptionRepository repository = mock(PushSubscriptionRepository.class);
    private final PushNotificationService service = new PushNotificationService(sender, repository);

    private PushSubscription assinatura(String endpoint) {
        PushSubscription s = new PushSubscription();
        s.setEndpoint(endpoint);
        s.setP256dh("pk");
        s.setAuth("au");
        s.setUser(new User());
        return s;
    }

    @Test
    void deveMandarParaTodasAsAssinaturas() throws Exception {
        when(sender.isConfigured()).thenReturn(true);
        when(repository.findByUserId("u1")).thenReturn(List.of(assinatura("https://a"), assinatura("https://b")));
        when(sender.send(any(), any(), any(), any())).thenReturn(201);

        service.notifyUser("u1", "Pedido #94c103db saiu para entrega", "João está a caminho", "/meus-pedidos");

        verify(sender).send(eq("https://a"), eq("pk"), eq("au"), argThat(json ->
                json.contains("\"title\":\"Pedido #94c103db saiu para entrega\"")
                        && json.contains("\"body\":\"João está a caminho\"")
                        && json.contains("\"operation\":\"navigateLastFocusedOrOpen\"")
                        && json.contains("\"url\":\"/meus-pedidos\"")));
        verify(sender).send(eq("https://b"), any(), any(), any());
        verify(repository, never()).delete(any());
    }

    @Test
    void deveApagarAssinaturaVencida() throws Exception {
        when(sender.isConfigured()).thenReturn(true);
        PushSubscription vencida = assinatura("https://vencida");
        PushSubscription naoExiste = assinatura("https://nao-existe");
        when(repository.findByUserId("u1")).thenReturn(List.of(vencida, naoExiste));
        when(sender.send(eq("https://vencida"), any(), any(), any())).thenReturn(410);
        when(sender.send(eq("https://nao-existe"), any(), any(), any())).thenReturn(404);

        service.notifyUser("u1", "t", "b", "/");

        verify(repository).delete(vencida);
        verify(repository).delete(naoExiste);
    }

    @Test
    void naoDeveMandarSemConfiguracao() {
        when(sender.isConfigured()).thenReturn(false);

        service.notifyUser("u1", "t", "b", "/");

        verifyNoInteractions(repository);
    }

    @Test
    void naoDeveQuebrarComFalha() throws Exception {
        when(sender.isConfigured()).thenReturn(true);
        when(repository.findByUserId("u1")).thenReturn(List.of(assinatura("https://a"), assinatura("https://b")));
        when(sender.send(eq("https://a"), any(), any(), any())).thenThrow(new RuntimeException("fora do ar"));
        when(sender.send(eq("https://b"), any(), any(), any())).thenReturn(201);

        assertDoesNotThrow(() -> service.notifyUser("u1", "t", "b", "/"));

        verify(sender).send(eq("https://b"), any(), any(), any()); // a falha de um aparelho não impede o outro
        verify(repository, never()).delete(any());
    }
}
