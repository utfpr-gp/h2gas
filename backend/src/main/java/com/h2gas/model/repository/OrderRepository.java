package com.h2gas.model.repository;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByCustomerId(String id, Pageable pageable);
    Page<Order> findBySupplierIdOrderByCreatedAtDesc(String id, Pageable pageable);
    Page<Order> findByStatus(OrderStatusEnum status, Pageable pageable);
    Page<Order> findBySupplierIdAndStatusOrderByCreatedAtDesc(String supplierId, OrderStatusEnum status, Pageable pageable);
    Page<Order> findBySupplierIdAndStatusNotInOrderByCreatedAtDesc(String supplierId,
                                                                   List<OrderStatusEnum> statuses,
                                                                   Pageable pageable);

}
