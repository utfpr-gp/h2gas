package com.h2gas.model.repository;

import com.h2gas.model.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, String> {
    Optional<Supplier> findByName(String name);
//    Optional<Supplier> findByUserId(String userId);
    Page<Supplier> findByActiveAndOpen(Boolean active, Boolean open, Pageable pageable);
    Page<Supplier> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Optional<Supplier> findByEmail(String email);
}
