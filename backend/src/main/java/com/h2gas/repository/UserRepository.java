package com.h2gas.repository;

import com.h2gas.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    UserDetails findByEmail(String email);
}
