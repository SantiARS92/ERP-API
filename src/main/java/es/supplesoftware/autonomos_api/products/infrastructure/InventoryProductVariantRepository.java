package es.supplesoftware.autonomos_api.products.infrastructure;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryProductVariantRepository extends JpaRepository<InventoryProductVariantEntity, Long> {

    Optional<InventoryProductVariantEntity> findByIdAndActiveTrue(Long id);

    Optional<InventoryProductVariantEntity> findByIdAndProductIdAndActiveTrue(Long id, Long productId);

    List<InventoryProductVariantEntity> findByProductIdOrderByVariantLabelAscIdAsc(Long productId);

    List<InventoryProductVariantEntity> findByProductIdAndActiveTrueOrderByVariantLabelAscIdAsc(Long productId);

    boolean existsByProductIdAndVariantLabelIgnoreCaseAndActiveTrue(Long productId, String variantLabel);

    boolean existsByProductIdAndVariantLabelIgnoreCaseAndActiveTrueAndIdNot(
            Long productId,
            String variantLabel,
            Long id
    );

    boolean existsBySkuIgnoreCaseAndActiveTrue(String sku);

    boolean existsBySkuIgnoreCaseAndActiveTrueAndIdNot(String sku, Long id);

    @Query("""
            select count(v) > 0
            from InventoryProductVariantEntity v
            where v.active = true
              and lower(v.sku) = lower(:sku)
              and v.product.id <> :productId
            """)
    boolean existsActiveSkuForOtherProduct(@Param("sku") String sku, @Param("productId") Long productId);
}
