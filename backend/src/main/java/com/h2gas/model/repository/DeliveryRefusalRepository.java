package com.h2gas.model.repository;

import com.h2gas.model.entity.DeliveryRefusal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryRefusalRepository extends JpaRepository<DeliveryRefusal, Long> {
    List<DeliveryRefusal> findByDelivererIdOrderByCreatedAtDesc(Long delivererId);
}
