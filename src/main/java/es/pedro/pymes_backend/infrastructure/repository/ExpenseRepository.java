package es.pedro.pymes_backend.infrastructure.repository;

import es.pedro.pymes_backend.domain.ExpenseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Long>, JpaSpecificationExecutor<ExpenseEntity> {

    @Query("""
            select distinct e.concept
            from ExpenseEntity e
            where lower(e.concept) like lower(concat('%', :query, '%'))
            order by e.concept asc
            """)
    List<String> findConceptSuggestions(@Param("query") String query, Pageable pageable);
}