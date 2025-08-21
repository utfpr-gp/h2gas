package com.h2gas.model.repository;

import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByCustomerId(String id, Pageable pageable);
}
