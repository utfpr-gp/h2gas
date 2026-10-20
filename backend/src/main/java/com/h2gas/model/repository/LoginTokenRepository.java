package com.h2gas.model.repository;

import com.h2gas.model.entity.LoginToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginTokenRepository extends JpaRepository<LoginToken, Long> {
    Optional<LoginToken> findByUserId(String userId);
}
