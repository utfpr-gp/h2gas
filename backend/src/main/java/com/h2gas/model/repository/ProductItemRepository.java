package com.h2gas.model.repository;

import com.h2gas.model.entity.ProductItem;
import com.h2gas.model.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductItemRepository extends JpaRepository<ProductItem, Long> {
    Page<ProductItem> findBySupplierId(Long id, Pageable pageable);
    Page<ProductItem> findBySupplierActiveAndSupplierOpen(Boolean active, Boolean open, Pageable pageable);

}
