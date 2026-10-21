package com.h2gas.model.repository;

import com.h2gas.model.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // F7 §5.25: média e contagem por loja, numa consulta só (sem N+1)
    @Query("SELECT r.supplier.id AS supplierId, AVG(r.rating) AS average, COUNT(r) AS total"
            + " FROM Review r WHERE r.supplier.id IN :supplierIds GROUP BY r.supplier.id")
    List<SupplierRatingSummary> summarizeBySupplierIds(@Param("supplierIds") Collection<String> supplierIds);

    interface SupplierRatingSummary {
        String getSupplierId();

        Double getAverage();

        Long getTotal();
    }
}
