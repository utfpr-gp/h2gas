package com.h2gas.model.repository;

import com.h2gas.model.dto.SupplierCategoryDTO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
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

    // HU15: tipos de produto das lojas da página da vitrine, numa consulta só (sem N+1).
    // A ordem alfabética é aplicada no serviço, com collation pt-BR: ordenar aqui dependeria
    // do banco (o H2 dos testes põe "Gás" antes de "Água"; o MySQL, o contrário).
    @Query("""
        SELECT DISTINCT c.supplier.id AS supplierId, t.id AS typeId, t.name AS typeName, t.icon AS typeIcon
        FROM CategoryProduct c JOIN c.productType t
        WHERE c.supplier.id IN :supplierIds
        """)
    List<SupplierProductTypeRow> findTypesBySupplierIds(@Param("supplierIds") Collection<String> supplierIds);

    interface SupplierProductTypeRow {
        String getSupplierId();
        Long getTypeId();
        String getTypeName();
        String getTypeIcon();
    }

    boolean existsBySupplierIdAndProductTypeIdAndNameIgnoreCase(String supplierId, Long productTypeId, String name);

    boolean existsBySupplierIdAndProductTypeIdAndNameIgnoreCaseAndIdNot(String supplierId, Long productTypeId, String name, Long id);
}
