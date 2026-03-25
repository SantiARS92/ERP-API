package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.ExpenseEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ExpenseSpecification {

    private ExpenseSpecification() {
    }

    public static Specification<ExpenseEntity> filter(LocalDate fromDate, LocalDate toDate, String search) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("expenseDate"), fromDate));
            }

            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("expenseDate"), toDate));
            }

            if (StringUtils.hasText(search)) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("concept")),
                                "%" + search.trim().toLowerCase() + "%"
                        )
                );
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}