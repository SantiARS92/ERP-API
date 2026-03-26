package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.InventoryProductEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class InventoryProductSpecification {

    private InventoryProductSpecification() {
    }

    public static Specification<InventoryProductEntity> filter(String search, List<Long> categoryIds) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.isTrue(root.get("active")));

            if (StringUtils.hasText(search)) {
                String normalized = "%" + search.trim().toLowerCase() + "%";

                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), normalized),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")), normalized)
                        )
                );
            }

            if (!CollectionUtils.isEmpty(categoryIds)) {
                predicates.add(root.get("category").get("id").in(categoryIds));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}