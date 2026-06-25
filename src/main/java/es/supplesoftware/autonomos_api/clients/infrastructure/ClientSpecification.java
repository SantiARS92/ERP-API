package es.supplesoftware.autonomos_api.clients.infrastructure;
import es.supplesoftware.autonomos_api.clients.infrastructure.ClientEntity;
import org.springframework.data.jpa.domain.Specification;

public class ClientSpecification {

    private ClientSpecification() {
    }

    public static Specification<ClientEntity> hasSearch(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            String searchLike = "%" + search.trim().toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), searchLike),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), searchLike),
                    criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("email"), "")), searchLike)
            );
        };
    }
}