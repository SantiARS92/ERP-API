package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.SaleEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class SaleSpecification {

    private SaleSpecification() {
    }

    public static Specification<SaleEntity> hasSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }

            String like = "%" + search.trim().toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("clientName")), like),
                    cb.like(cb.lower(root.get("itemName")), like)
            );
        };
    }

    public static Specification<SaleEntity> saleDateFrom(LocalDate fromDate) {
        return (root, query, cb) -> {
            if (fromDate == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("saleDate"), fromDate);
        };
    }

    public static Specification<SaleEntity> saleDateTo(LocalDate toDate) {
        return (root, query, cb) -> {
            if (toDate == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("saleDate"), toDate);
        };
    }
}