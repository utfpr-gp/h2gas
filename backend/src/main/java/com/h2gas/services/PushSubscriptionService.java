package com.h2gas.services;

import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.PushSubscriptionRequestDTO;
import com.h2gas.model.entity.PushSubscription;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.PushSubscriptionRepository;
import com.h2gas.model.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU27: cada usuário grava e apaga só as próprias assinaturas
@Service
public class PushSubscriptionService {

    private final PushSubscriptionRepository repository;
    private final UserRepository userRepository;

    public PushSubscriptionService(PushSubscriptionRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    // o mesmo endpoint passa a ser do usuário atual (o aparelho trocou de conta)
    @Transactional
    public void subscribe(String userId, PushSubscriptionRequestDTO dto) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        PushSubscription subscription = repository.findByEndpoint(dto.endpoint()).orElseGet(PushSubscription::new);
        subscription.setUser(user);
        subscription.setEndpoint(dto.endpoint());
        subscription.setP256dh(dto.keys().p256dh());
        subscription.setAuth(dto.keys().auth());
        repository.save(subscription);
    }

    @Transactional
    public void unsubscribe(String userId, String endpoint) {
        repository.findByEndpoint(endpoint)
                .filter(s -> s.getUser().getId().equals(userId))
                .ifPresent(repository::delete);
    }
}
