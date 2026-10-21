package com.h2gas.model.repository;

import com.h2gas.model.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // F7 §5.25: média e contagem por loja, numa consulta só (sem N+1)
    @Query("SELECT r.supplier.id AS supplierId, AVG(r.rating) AS average, COUNT(r) AS total"
            + " FROM Review r WHERE r.supplier.id IN :supplierIds GROUP BY r.supplier.id")
    List<SupplierRatingSummary> summarizeBySupplierIds(@Param("supplierIds") Collection<String> supplierIds);

    // F7 §5.27: avaliações da loja, da entrega mais recente para a mais antiga
    @Query(value = "SELECT r.id AS id, c.name AS customerName, r.rating AS rating, r.comment AS comment, o.deliveredAt AS deliveredAt"
            + " FROM Review r JOIN r.order o JOIN o.customer c WHERE r.supplier.id = :supplierId"
            + " ORDER BY o.deliveredAt DESC, r.id DESC",
            countQuery = "SELECT COUNT(r) FROM Review r WHERE r.supplier.id = :supplierId")
    Page<StoreReviewRow> findStoreReviews(@Param("supplierId") String supplierId, Pageable pageable);

    interface StoreReviewRow {
        Long getId();

        String getCustomerName();

        Integer getRating();

        String getComment();

        LocalDateTime getDeliveredAt();
    }

    interface SupplierRatingSummary {
        String getSupplierId();

        Double getAverage();

        Long getTotal();
    }
}
