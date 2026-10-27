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

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

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
    long countByCategoryProductId(Long categoryProductId);
    long countBySupplierIdAndStockQuantityGreaterThan(String supplierId, int stock);
    long countBySupplierIdAndStockQuantity(String supplierId, int stock);
    long countBySupplierIdAndStockQuantityBetween(String supplierId, int min, int max);

    // HU18: a loja que vende precisa estar aberta agora pelo horário (mesma regra de SupplierRepository.OPEN_NOW)
    String SUPPLIER_OPEN_NOW = "(p.supplier.openingTime IS NOT NULL AND p.supplier.closingTime IS NOT NULL AND ("
            + "(p.supplier.openingTime < p.supplier.closingTime"
            + " AND :now >= p.supplier.openingTime AND :now < p.supplier.closingTime)"
            + " OR (p.supplier.openingTime > p.supplier.closingTime"
            + " AND (:now >= p.supplier.openingTime OR :now < p.supplier.closingTime))))";

    // HU19: tipos de produto que cada loja da página vende (produtos com estoque, RN02), numa consulta só (sem N+1).
    // A ordem alfabética é aplicada no serviço, com collation pt-BR: o banco ordena "Gás" antes de "Água" no H2.
    @Query("""
        SELECT DISTINCT p.supplier.id AS supplierId, t.id AS typeId, t.name AS typeName, t.icon AS typeIcon
        FROM ProductItem p JOIN p.categoryProduct c JOIN c.productType t
        WHERE p.supplier.id IN :supplierIds AND p.stockQuantity > 0
        """)
    List<SupplierProductTypeRow> findTypesBySupplierIds(@Param("supplierIds") Collection<String> supplierIds);

    interface SupplierProductTypeRow {
        String getSupplierId();
        Long getTypeId();
        String getTypeName();
        String getTypeIcon();
    }

    String AVAILABLE_WHERE = " WHERE p.supplier.active = true AND " + SUPPLIER_OPEN_NOW
            + " AND (:cityId IS NULL OR p.supplier.city.id = :cityId)"
            + " AND (:category IS NULL OR LOWER(p.categoryProduct.productType.name) = LOWER(:category))"
            + " AND p.stockQuantity > 0"
            + " AND (:q IS NULL OR LOCATE(:q, LOWER(p.brand)) > 0"
            + " OR LOCATE(:q, LOWER(p.categoryProduct.name)) > 0"
            + " OR LOCATE(:q, LOWER(p.categoryProduct.productType.name)) > 0)";

    // HU16: byShopRating ordena pela nota da loja que vende — produto não tem avaliação própria —, com loja sem
    // nota no fim. Fora disso os dois critérios viram constantes e a ordem vem do Sort do Pageable.
    @Query(value = "SELECT p FROM ProductItem p" + AVAILABLE_WHERE
            + " ORDER BY CASE WHEN :byShopRating = true"
            + " AND (SELECT AVG(r.rating) FROM Review r WHERE r.supplier = p.supplier) IS NULL THEN 1 ELSE 0 END,"
            + " CASE WHEN :byShopRating = true"
            + " THEN (SELECT AVG(r2.rating) FROM Review r2 WHERE r2.supplier = p.supplier) END DESC",
            countQuery = "SELECT COUNT(p) FROM ProductItem p" + AVAILABLE_WHERE)
    Page<ProductItem> findAvailableProducts(@Param("cityId") Long cityId,
                                             @Param("category") String category,
                                             @Param("q") String q,
                                             @Param("byShopRating") boolean byShopRating,
                                             @Param("now") LocalTime now,
                                             Pageable pageable);

    // F7 §5.21: busca opcional (q em minúsculas) por marca, categoria ou tipo; LOCATE pelo mesmo motivo da HU12
    default Page<ProductItem> findAvailableProducts(Long cityId, String category, LocalTime now, Pageable pageable) {
        return findAvailableProducts(cityId, category, null, false, now, pageable);
    }

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
