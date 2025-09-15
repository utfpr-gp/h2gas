package com.h2gas.model.repository;

import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
//    UserDetails findByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByName(String name);
    boolean existsByEmail(String email);
}
