package com.h2gas.model.repository;

import com.h2gas.model.entity.Deliverer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DelivererRepository extends JpaRepository<Deliverer, Long> {
    Optional<Deliverer> findByUserId(String userId);

    List<Deliverer> findBySupplierIdOrderByUserNameAsc(String supplierId);
}
