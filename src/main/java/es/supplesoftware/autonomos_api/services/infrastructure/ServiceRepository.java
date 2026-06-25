package es.supplesoftware.autonomos_api.services.infrastructure;
import es.supplesoftware.autonomos_api.services.infrastructure.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long>, JpaSpecificationExecutor<ServiceEntity> {

    long countByCategoryId(Long categoryId);

    boolean existsByCategoryId(Long categoryId);
}