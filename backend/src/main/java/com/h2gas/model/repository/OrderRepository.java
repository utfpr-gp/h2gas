package com.h2gas.model.repository;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByCustomerId(String id, Pageable pageable);
    Page<Order> findBySupplierIdOrderByCreatedAtDesc(String id, Pageable pageable);
    Page<Order> findByStatus(OrderStatusEnum status, Pageable pageable);
    // HU09: pedidos do mês com itens e produtos já carregados (a agregação é feita em Java)
    @EntityGraph(attributePaths = {"items", "items.product", "items.product.categoryProduct", "items.product.categoryProduct.productType"})
    @Query("SELECT o FROM Order o WHERE o.supplier.id = :supplierId AND o.createdAt >= :start AND o.createdAt < :end")
    List<Order> findForDashboard(@Param("supplierId") String supplierId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    long countBySupplierIdAndStatus(String supplierId, OrderStatusEnum status);

    boolean existsByDelivererIdAndStatus(Long delivererId, OrderStatusEnum status);
    List<Order> findByDelivererIdAndStatusOrderByDispatchedAtAsc(Long delivererId, OrderStatusEnum status);
    List<Order> findByDelivererIdAndStatusOrderByCreatedAtAsc(Long delivererId, OrderStatusEnum status);
    List<Order> findByDelivererIdAndStatusOrderByDeliveredAtDesc(Long delivererId, OrderStatusEnum status);
    Page<Order> findBySupplierIdAndStatusOrderByCreatedAtDesc(String supplierId, OrderStatusEnum status, Pageable pageable);
    Page<Order> findBySupplierIdAndStatusNotInOrderByCreatedAtDesc(String supplierId,
                                                                   List<OrderStatusEnum> statuses,
                                                                   Pageable pageable);
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);
    Page<Order> findByCustomerIdAndStatusOrderByCreatedAtDesc(String customerId,
                                                              OrderStatusEnum status,
                                                              Pageable pageable);


}
