package com.h2gas.model.repository;

import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    // HU23: todos os status da loja numa consulta só
    @Query("SELECT o.status AS status, COUNT(o) AS total, COALESCE(SUM(o.totalPrice), 0) AS amount "
            + "FROM Order o WHERE o.supplier.id = :supplierId GROUP BY o.status")
    List<StatusCountRow> countBySupplierGroupedByStatus(@Param("supplierId") String supplierId);

    interface StatusCountRow {
        OrderStatusEnum getStatus();
        Long getTotal();
        java.math.BigDecimal getAmount(); // HU28: soma do total dos pedidos no status
    }

    // HU12: painel do admin (plataforma inteira)
    long countByStatus(OrderStatusEnum status);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o "
            + "WHERE o.createdAt >= :start AND o.createdAt < :end AND o.status <> :excluded")
    BigDecimal sumTotalPriceCreatedBetweenExcludingStatus(@Param("start") LocalDateTime start,
                                                          @Param("end") LocalDateTime end,
                                                          @Param("excluded") OrderStatusEnum excluded);

    boolean existsByDelivererIdAndStatus(Long delivererId, OrderStatusEnum status);
    List<Order> findByDelivererIdAndStatusOrderByDispatchedAtAsc(Long delivererId, OrderStatusEnum status);
    List<Order> findByDelivererIdAndStatusOrderByCreatedAtAsc(Long delivererId, OrderStatusEnum status);
    List<Order> findByDelivererIdAndStatusOrderByDeliveredAtDesc(Long delivererId, OrderStatusEnum status);
    Page<Order> findBySupplierIdAndStatusOrderByCreatedAtDesc(String supplierId, OrderStatusEnum status, Pageable pageable);
    // HU28: pedidos em aberto por ordem de chegada (o mais antigo primeiro)
    Page<Order> findBySupplierIdAndStatusOrderByCreatedAtAsc(String supplierId, OrderStatusEnum status, Pageable pageable);
    Page<Order> findBySupplierIdAndStatusNotInOrderByCreatedAtAsc(String supplierId,
                                                                   List<OrderStatusEnum> statuses,
                                                                   Pageable pageable);
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);
    Page<Order> findByCustomerIdAndStatusOrderByCreatedAtDesc(String customerId,
                                                              OrderStatusEnum status,
                                                              Pageable pageable);



    // F7 fatia 6: histórico do fornecedor. LOCATE em vez de LIKE: o Spring Data reescreve o parâmetro do LIKE
    // e a contagem da paginação perde a reescrita (mesmo motivo da busca de usuários da HU12)
    String HISTORY_WHERE = """
        o.supplier.id = :supplierId AND o.status IN :statuses
          AND (:from IS NULL OR o.createdAt >= :from)
          AND (:to IS NULL OR o.createdAt < :to)
          AND (:payment IS NULL OR o.paymentMethod = :payment)
          AND (:mode IS NULL OR o.deliveryMode = :mode)
          AND (:q IS NULL OR LOCATE(:q, LOWER(o.customer.name)) > 0 OR LOCATE(:q, LOWER(o.id)) = 1)
        """;

    @Query("SELECT o FROM Order o WHERE " + HISTORY_WHERE + " ORDER BY o.createdAt DESC")
    Page<Order> findHistory(@Param("supplierId") String supplierId, @Param("statuses") List<OrderStatusEnum> statuses,
                            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                            @Param("payment") String payment, @Param("mode") DeliveryModeEnum mode,
                            @Param("q") String q, Pageable pageable);

    @Query("SELECT COUNT(o), COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE " + HISTORY_WHERE)
    List<Object[]> summarizeHistory(@Param("supplierId") String supplierId, @Param("statuses") List<OrderStatusEnum> statuses,
                                    @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                    @Param("payment") String payment, @Param("mode") DeliveryModeEnum mode,
                                    @Param("q") String q);
}
