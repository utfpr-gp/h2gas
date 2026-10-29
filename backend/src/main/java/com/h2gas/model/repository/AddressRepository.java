package com.h2gas.model.repository;

import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    Optional<Address> findByStreetName(String name);
    boolean existsByCityId(Long cityId);

    // usuário pode ter N endereços (F3); quem só precisa de um usa o mais recente
    Optional<Address> findFirstByUserIdOrderByIdDesc(String id);

    List<Address> findAllByUserIdOrderByIdDesc(String userId);

    // HU17: rotina de subida que preenche as coordenadas dos endereços antigos
    List<Address> findByLatitudeIsNull();
}
