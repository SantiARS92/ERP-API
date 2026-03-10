package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.ServiceEntity;
import org.springframework.data.jpa.domain.Specification;

public class ServiceSpecification {

    public static Specification<ServiceEntity> hasSearch(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank()) {
                return null;
            }

            String like = "%" + search.toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), like),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), like)
            );
        };
    }

    public static Specification<ServiceEntity> hasActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (active == null) {
                return null;
            }

            return criteriaBuilder.equal(root.get("active"), active);
        };
    }

    public static Specification<ServiceEntity> hasCategoryId(Long categoryId) {
        return (root, query, criteriaBuilder) -> {
            if (categoryId == null) {
                return null;
            }

            return criteriaBuilder.equal(root.get("category").get("id"), categoryId);
        };
    }

    public static Specification<ServiceEntity> hasMinPrice(Double minPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null) {
                return null;
            }

            return criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
        };
    }

    public static Specification<ServiceEntity> hasMaxPrice(Double maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (maxPrice == null) {
                return null;
            }

            return criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }
}