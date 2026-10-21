package com.h2gas.model.repository;

import com.h2gas.model.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {
    Optional<City> findByName(String name);
    boolean existsByIbgeCode(Integer ibgeCode);
    boolean existsByNameIgnoreCaseAndStateIgnoreCase(String name, String state);
}
