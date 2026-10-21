package com.h2gas.model.repository;

import com.h2gas.model.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, String> {
    Optional<Supplier> findByName(String name);
//    Optional<Supplier> findByUserId(String userId);
    Page<Supplier> findByActiveAndOpen(Boolean active, Boolean open, Pageable pageable);
    Page<Supplier> findByCityIdAndActiveAndOpen(Long cityId, Boolean active, Boolean open, Pageable pageable);
    Page<Supplier> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Optional<Supplier> findByEmail(String email);
    long countByActive(boolean active);
    boolean existsByCityId(Long cityId);

    // F7 §5.21: catálogo do cliente — lojas ativas e abertas, da cidade do endereço (RN05), com busca opcional pelo nome
    @Query("SELECT s FROM Supplier s WHERE s.active = true AND s.open = true"
            + " AND (:cityId IS NULL OR s.city.id = :cityId)"
            + " AND (:q IS NULL OR LOCATE(:q, LOWER(s.name)) > 0) ORDER BY s.name")
    Page<Supplier> findOpenForCatalog(@Param("cityId") Long cityId, @Param("q") String q, Pageable pageable);

    // F7 fatia 6: aprovação de fornecedores — status e busca opcionais, aplicados no banco para a paginação bater
    @Query("select s from Supplier s where (:active is null or s.active = :active) and (:q is null"
            + " or lower(s.name) like lower(concat('%', :q, '%'))"
            + " or lower(s.email) like lower(concat('%', :q, '%'))"
            + " or s.cnpj like concat('%', :q, '%')) order by s.name")
    Page<Supplier> findForApproval(@Param("active") Boolean active, @Param("q") String q, Pageable pageable);
}
