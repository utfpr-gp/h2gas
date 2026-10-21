package com.h2gas.model.repository;

import com.h2gas.model.entity.ProductItem;
import com.h2gas.model.entity.Supplier;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductItemRepository extends JpaRepository<ProductItem, Long> {
    Page<ProductItem> findBySupplierId(String id, Pageable pageable);

    // F7 fatia 6: lista do fornecedor; minStock/maxStock nulos = sem limite
    @Query("""
        SELECT p FROM ProductItem p
        WHERE p.supplier.id = :supplierId
          AND (:q IS NULL OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.categoryProduct.name) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.categoryProduct.productType.name) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:minStock IS NULL OR p.stockQuantity >= :minStock)
          AND (:maxStock IS NULL OR p.stockQuantity <= :maxStock)
        ORDER BY LOWER(p.brand), p.id
        """)
    Page<ProductItem> findMine(@Param("supplierId") String supplierId,
                               @Param("q") String q,
                               @Param("minStock") Integer minStock,
                               @Param("maxStock") Integer maxStock,
                               Pageable pageable);

    long countBySupplierId(String supplierId);
    long countBySupplierIdAndStockQuantityGreaterThan(String supplierId, int stock);
    long countBySupplierIdAndStockQuantity(String supplierId, int stock);
    long countBySupplierIdAndStockQuantityBetween(String supplierId, int min, int max);

    @Query("""
        SELECT p FROM ProductItem p
        WHERE p.supplier.active = true AND p.supplier.open = true
          AND (:cityId IS NULL OR p.supplier.city.id = :cityId)
          AND (:category IS NULL OR LOWER(p.categoryProduct.productType.name) = LOWER(:category))
          AND p.stockQuantity > 0
        """)
    Page<ProductItem> findAvailableProducts(@Param("cityId") Long cityId,
                                             @Param("category") String category,
                                             Pageable pageable);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT p FROM ProductItem p WHERE p.id = :id")
    ProductItem findByIdWithLock(@Param("id") Long id);

    @Query("""
        SELECT p FROM ProductItem p
        WHERE p.supplier.id = :supplierId
          AND (:category IS NULL OR LOWER(p.categoryProduct.productType.name) = LOWER(:category))
          AND (:search IS NULL OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:onlyAvailable = FALSE OR p.stockQuantity > 0)
        """)
    Page<ProductItem> findBySupplierIdFiltered(@Param("supplierId") String supplierId,
                                                @Param("category") String category,
                                                @Param("search") String search,
                                                @Param("onlyAvailable") boolean onlyAvailable,
                                                Pageable pageable);
}
