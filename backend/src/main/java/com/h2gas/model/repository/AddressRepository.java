package com.h2gas.model.repository;

import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    Optional<Address> findByStreetName(String name);

    /**
     * Um usuário pode ter múltiplos endereços (F3). Até a HU04 entregar a
     * gestão completa de múltiplos endereços, o "endereço ativo" usado por
     * contextos que ainda assumem um único endereço (HU01) é o mais
     * recente, para evitar IncorrectResultSizeDataAccessException.
     */
    Optional<Address> findFirstByUserIdOrderByIdDesc(String id);
}
