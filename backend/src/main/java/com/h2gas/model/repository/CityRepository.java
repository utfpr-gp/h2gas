package com.h2gas.model.repository;

import com.h2gas.model.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, String> { }
