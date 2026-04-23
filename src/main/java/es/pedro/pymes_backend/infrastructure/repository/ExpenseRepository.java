package es.pedro.pymes_backend.infrastructure.repository;

import es.pedro.pymes_backend.domain.ExpenseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Long>, JpaSpecificationExecutor<ExpenseEntity> {

    @Query("""
            select e
            from ExpenseEntity e
            left join fetch e.inventoryProduct
            left join fetch e.inventoryProductVariant v
            left join fetch v.product
            where e.id = :id
            """)
    Optional<ExpenseEntity> findByIdWithInventoryReferences(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {
            "inventoryProduct",
            "inventoryProductVariant",
            "inventoryProductVariant.product"
    })
    Page<ExpenseEntity> findAll(Specification<ExpenseEntity> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "inventoryProduct",
            "inventoryProductVariant",
            "inventoryProductVariant.product"
    })
    List<ExpenseEntity> findAll(Specification<ExpenseEntity> spec, Sort sort);

    @Query("""
            select e
            from ExpenseEntity e
            left join fetch e.inventoryProduct
            left join fetch e.inventoryProductVariant v
            left join fetch v.product
            order by e.expenseDate desc, e.id desc
            """)
    List<ExpenseEntity> findRecentWithInventoryReferences(Pageable pageable);

    @Query("""
            select distinct e.concept
            from ExpenseEntity e
            where lower(e.concept) like lower(concat('%', :query, '%'))
            order by e.concept asc
            """)
    List<String> findConceptSuggestions(@Param("query") String query, Pageable pageable);
}
