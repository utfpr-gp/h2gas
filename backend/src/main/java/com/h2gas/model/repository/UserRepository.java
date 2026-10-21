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

    // HU12: term já vem em minúsculas, ou null para não filtrar. LOCATE em vez de LIKE: o Spring Data
    // reescreve o parâmetro do LIKE e a consulta de contagem da paginação perde essa reescrita
    @Query("SELECT u FROM User u WHERE (:type IS NULL OR u.userType = :type) "
            + "AND (:term IS NULL OR LOCATE(:term, LOWER(u.name)) > 0 OR LOCATE(:term, LOWER(u.email)) > 0)")
    Page<User> searchForAdmin(@Param("type") UserTypeEnum type, @Param("term") String term, Pageable pageable);

    long countByUserType(UserTypeEnum userType);
}
