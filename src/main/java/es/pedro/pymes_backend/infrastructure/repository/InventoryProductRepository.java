package es.pedro.pymes_backend.infrastructure.repository;

import es.pedro.pymes_backend.domain.InventoryProductEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryProductRepository extends JpaRepository<InventoryProductEntity, Long>,
        JpaSpecificationExecutor<InventoryProductEntity> {

    Optional<InventoryProductEntity> findByIdAndActiveTrue(Long id);

    Optional<InventoryProductEntity> findByNameIgnoreCaseAndActiveTrue(String name);

    @Query("""
            select p.name
            from InventoryProductEntity p
            where p.active = true
              and lower(p.name) like lower(concat('%', :query, '%'))
            order by p.name asc
            """)
    List<String> findActiveNameSuggestions(@Param("query") String query, Pageable pageable);

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);

    boolean existsByNameIgnoreCaseAndActiveTrueAndIdNot(String name, Long id);

    boolean existsBySkuIgnoreCaseAndActiveTrue(String sku);

    boolean existsBySkuIgnoreCaseAndActiveTrueAndIdNot(String sku, Long id);
}
