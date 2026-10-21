package com.h2gas.model.repository;

import com.h2gas.model.entity.EmailChangeToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, Long> {
    Optional<EmailChangeToken> findByUserId(String userId);
}
