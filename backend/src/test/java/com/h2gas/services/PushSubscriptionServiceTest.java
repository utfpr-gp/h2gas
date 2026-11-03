package com.h2gas.services;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.PushSubscriptionRequestDTO;
import com.h2gas.model.entity.PushSubscription;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.PushSubscriptionRepository;
import com.h2gas.model.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

// HU27: assinaturas de push de cada usuário
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PushSubscriptionServiceTest {

    @Autowired private PushSubscriptionService service;
    @Autowired private PushSubscriptionRepository repository;
    @Autowired private UserRepository userRepository;

    private User maria;
    private User joao;

    @BeforeEach
    void setUp() {
        maria = userRepository.save(new User(null, "Maria", "maria.hu27@teste.com", "41999990001", "11111111111", "s", UserTypeEnum.CUSTOMER));
        joao = userRepository.save(new User(null, "João", "joao.hu27@teste.com", "41999990002", "22222222222", "s", UserTypeEnum.SUPPLIER));
    }

    private PushSubscriptionRequestDTO assinatura(String endpoint) {
        return new PushSubscriptionRequestDTO(endpoint, new PushSubscriptionRequestDTO.Keys("chave-p256dh", "chave-auth"));
    }

    @Test
    void deveGravarAAssinaturaDoUsuario() {
        service.subscribe(maria.getId(), assinatura("https://fcm.googleapis.com/fcm/send/abc"));
        service.subscribe(maria.getId(), assinatura("https://fcm.googleapis.com/fcm/send/abc")); // de novo não duplica

        var salvas = repository.findByUserId(maria.getId());
        assertEquals(1, salvas.size());
        assertEquals("chave-p256dh", salvas.get(0).getP256dh());
        assertEquals("chave-auth", salvas.get(0).getAuth());
    }

    @Test
    void devePassarOEndpointParaOUsuarioAtual() {
        service.subscribe(maria.getId(), assinatura("https://fcm.googleapis.com/fcm/send/aparelho"));

        service.subscribe(joao.getId(), assinatura("https://fcm.googleapis.com/fcm/send/aparelho"));

        assertTrue(repository.findByUserId(maria.getId()).isEmpty());
        assertEquals(1, repository.findByUserId(joao.getId()).size());
    }

    @Test
    void deveApagarSoAPropria() {
        service.subscribe(maria.getId(), assinatura("https://fcm.googleapis.com/fcm/send/da-maria"));

        service.unsubscribe(joao.getId(), "https://fcm.googleapis.com/fcm/send/da-maria");
        assertEquals(1, repository.findByUserId(maria.getId()).size());

        service.unsubscribe(maria.getId(), "https://fcm.googleapis.com/fcm/send/da-maria");
        assertTrue(repository.findByUserId(maria.getId()).isEmpty());
        PushSubscription nenhuma = repository.findByEndpoint("https://fcm.googleapis.com/fcm/send/da-maria").orElse(null);
        assertNull(nenhuma);
    }
}
