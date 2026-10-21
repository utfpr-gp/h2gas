package com.h2gas.model.repository;

import com.h2gas.model.dto.SupplierCategoryDTO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.CategoryProduct;
import com.h2gas.model.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryProductRepository extends JpaRepository<CategoryProduct, Long> {
    Page<CategoryProduct> findBySupplierId(String id,  Pageable pageable);

    // F7 fatia 6: categorias da loja com a contagem de produtos numa consulta só
    @Query("""
        SELECT new com.h2gas.model.dto.SupplierCategoryDTO(c.id, c.name, t.name, t.id, t.icon,
               (SELECT COUNT(p) FROM ProductItem p WHERE p.categoryProduct = c))
        FROM CategoryProduct c JOIN c.productType t
        WHERE c.supplier.id = :supplierId
        ORDER BY LOWER(t.name), LOWER(c.name)
        """)
    List<SupplierCategoryDTO> findMine(@Param("supplierId") String supplierId);

    boolean existsBySupplierIdAndProductTypeIdAndNameIgnoreCase(String supplierId, Long productTypeId, String name);

    boolean existsBySupplierIdAndProductTypeIdAndNameIgnoreCaseAndIdNot(String supplierId, Long productTypeId, String name, Long id);
}
