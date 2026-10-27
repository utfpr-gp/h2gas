package com.h2gas.model.repository;

import com.h2gas.enums.StorefrontSortEnum;
import com.h2gas.model.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, String> {
    Optional<Supplier> findByName(String name);
//    Optional<Supplier> findByUserId(String userId);
    Page<Supplier> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Optional<Supplier> findByEmail(String email);
    long countByActive(boolean active);
    boolean existsByCityId(Long cityId);

    // F7 §5.21: catálogo do cliente — lojas ativas e abertas, da cidade do endereço (RN05), com busca opcional pelo nome
    // HU15: productTypeId opcional — só as lojas que têm alguma categoria daquele tipo (Gás, Água…)
    // HU18: aberta agora pelo horário — mesma regra de OpeningHours.isOpenAt; :now é a hora de Brasília (Clock).
    // A abertura conta e o fechamento não; fechamento antes da abertura = vira a meia-noite; sem horário = fechada.
    String OPEN_NOW = "(s.openingTime IS NOT NULL AND s.closingTime IS NOT NULL AND ("
            + "(s.openingTime < s.closingTime AND :now >= s.openingTime AND :now < s.closingTime)"
            + " OR (s.openingTime > s.closingTime AND (:now >= s.openingTime OR :now < s.closingTime))))";

    // HU18: a vitrine traz também as lojas fechadas (includeClosed), sempre depois das abertas
    String CATALOG_WHERE = " WHERE s.active = true AND (:includeClosed = true OR " + OPEN_NOW + ")"
            + " AND (:cityId IS NULL OR s.city.id = :cityId)"
            + " AND (:q IS NULL OR LOCATE(:q, LOWER(s.name)) > 0)"
            + " AND (:productTypeId IS NULL OR EXISTS (SELECT 1 FROM CategoryProduct c"
            + " WHERE c.supplier = s AND c.productType.id = :productTypeId))";

    // HU16: a ordem vem de :sort (StorefrontSortEnum.param). Cada critério só pesa na ordenação dele e vira
    // constante nas outras; nota e tempo vazios vão para o fim, e o nome desempata sempre.
    // HU18: antes de tudo, abertas agora antes das fechadas.
    @Query(value = "SELECT s FROM Supplier s" + CATALOG_WHERE
            + " ORDER BY CASE WHEN " + OPEN_NOW + " THEN 0 ELSE 1 END,"
            + " CASE WHEN :sort = 'rating' AND (SELECT AVG(r.rating) FROM Review r WHERE r.supplier = s) IS NULL"
            + " THEN 1 ELSE 0 END,"
            + " CASE WHEN :sort = 'rating' THEN (SELECT AVG(r2.rating) FROM Review r2 WHERE r2.supplier = s) END DESC,"
            + " CASE WHEN :sort = 'deliveryTime' AND s.averageDeliveryTime IS NULL THEN 1 ELSE 0 END,"
            + " CASE WHEN :sort = 'deliveryTime' THEN s.averageDeliveryTime END,"
            + " s.name",
            countQuery = "SELECT COUNT(s) FROM Supplier s" + CATALOG_WHERE)
    Page<Supplier> findForCatalog(@Param("cityId") Long cityId, @Param("q") String q,
                                  @Param("productTypeId") Long productTypeId,
                                  @Param("includeClosed") boolean includeClosed,
                                  @Param("sort") String sort, @Param("now") LocalTime now, Pageable pageable);

    default Page<Supplier> findOpenForCatalog(Long cityId, String q, Long productTypeId, LocalTime now, Pageable pageable) {
        return findForCatalog(cityId, q, productTypeId, false, StorefrontSortEnum.NAME.param(), now, pageable);
    }

    // F7 fatia 6: aprovação de fornecedores — status e busca opcionais, aplicados no banco para a paginação bater
    @Query("select s from Supplier s where (:active is null or s.active = :active) and (:q is null"
            + " or lower(s.name) like lower(concat('%', :q, '%'))"
            + " or lower(s.email) like lower(concat('%', :q, '%'))"
            + " or s.cnpj like concat('%', :q, '%')) order by s.name")
    Page<Supplier> findForApproval(@Param("active") Boolean active, @Param("q") String q, Pageable pageable);
}
