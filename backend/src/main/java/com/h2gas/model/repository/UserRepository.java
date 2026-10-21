package com.h2gas.model.repository;

import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.User;
import com.h2gas.enums.UserTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByName(String name);
    boolean existsByEmail(String email);

    // HU12: pattern já vem em minúsculas e com os curingas (%termo%), ou null para não filtrar
    @Query("SELECT u FROM User u WHERE (:type IS NULL OR u.userType = :type) "
            + "AND (:pattern IS NULL OR LOWER(u.name) LIKE :pattern OR LOWER(u.email) LIKE :pattern)")
    Page<User> searchForAdmin(@Param("type") UserTypeEnum type, @Param("pattern") String pattern, Pageable pageable);
}
