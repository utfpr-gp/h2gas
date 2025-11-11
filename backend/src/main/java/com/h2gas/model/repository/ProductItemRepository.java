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
    Page<ProductItem> findBySupplierActiveAndSupplierOpen(Boolean active, Boolean open, Pageable pageable);
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT p FROM ProductItem p WHERE p.id = :id")
    ProductItem findByIdWithLock(@Param("id") Long id);

//    Page<ProductItem> findBySupplierIdAndCategoryProduct_NameIgnoreCase(
//            String supplierId, String category, Pageable pageable);
    Page<ProductItem> findBySupplierIdAndCategoryProduct_ProductType_NameIgnoreCase(
        String supplierId, String productTypeName, Pageable pageable);

    Page<ProductItem> findBySupplierIdAndBrandContainingIgnoreCase(
            String supplierId, String brand, Pageable pageable);
}
