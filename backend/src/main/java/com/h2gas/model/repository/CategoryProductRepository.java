package com.h2gas.model.repository;

import com.h2gas.model.dto.SupplierCategoryDTO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

import com.h2gas.model.entity.CategoryProduct;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CategoryProductRepository extends JpaRepository<CategoryProduct, Long> {

    // HU19: categorias que a loja usa (as dos produtos dela), com a contagem dos produtos dela, numa consulta só
    @Query("""
        SELECT new com.h2gas.model.dto.SupplierCategoryDTO(c.id, c.name, t.name, t.id, t.icon, COUNT(p))
        FROM ProductItem p JOIN p.categoryProduct c JOIN c.productType t
        WHERE p.supplier.id = :supplierId
        GROUP BY c.id, c.name, t.name, t.id, t.icon
        ORDER BY LOWER(t.name), LOWER(c.name)
        """)
    List<SupplierCategoryDTO> findMine(@Param("supplierId") String supplierId);

    // HU19: a categoria é global — nome único por tipo
    boolean existsByProductTypeIdAndNameIgnoreCase(Long productTypeId, String name);

    boolean existsByProductTypeIdAndNameIgnoreCaseAndIdNot(Long productTypeId, String name, Long id);
}
