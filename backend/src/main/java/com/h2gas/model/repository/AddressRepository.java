package com.h2gas.model.repository;

import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    Optional<Address> findByStreetName(String name);
}
