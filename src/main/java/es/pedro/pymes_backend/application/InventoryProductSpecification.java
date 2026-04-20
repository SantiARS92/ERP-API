package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.domain.InventoryProductVariantEntity;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
                Subquery<Long> variantSubquery = query.subquery(Long.class);
                Root<InventoryProductVariantEntity> variantRoot = variantSubquery.from(InventoryProductVariantEntity.class);

                variantSubquery.select(variantRoot.get("product").get("id"))
                        .where(
                                criteriaBuilder.equal(variantRoot.get("product").get("id"), root.get("id")),
                                criteriaBuilder.isTrue(variantRoot.get("active")),
                                criteriaBuilder.or(
                                        criteriaBuilder.like(criteriaBuilder.lower(variantRoot.get("variantLabel")), normalized),
                                        criteriaBuilder.like(criteriaBuilder.lower(variantRoot.get("sku")), normalized)
                                )
                        );

                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), normalized),
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")), normalized),
                                criteriaBuilder.exists(variantSubquery)
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
