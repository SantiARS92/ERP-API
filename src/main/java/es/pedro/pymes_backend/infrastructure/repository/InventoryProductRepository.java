package es.pedro.pymes_backend.infrastructure.repository;

import es.pedro.pymes_backend.domain.InventoryProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface InventoryProductRepository extends JpaRepository<InventoryProductEntity, Long>,
        JpaSpecificationExecutor<InventoryProductEntity> {

    Optional<InventoryProductEntity> findByIdAndActiveTrue(Long id);

    Optional<InventoryProductEntity> findByNameIgnoreCaseAndActiveTrue(String name);

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);

    boolean existsByNameIgnoreCaseAndActiveTrueAndIdNot(String name, Long id);

    boolean existsBySkuIgnoreCaseAndActiveTrue(String sku);

    boolean existsBySkuIgnoreCaseAndActiveTrueAndIdNot(String sku, Long id);
}