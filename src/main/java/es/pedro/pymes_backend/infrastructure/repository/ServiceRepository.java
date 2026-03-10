package es.pedro.pymes_backend.infrastructure.repository;

import es.pedro.pymes_backend.domain.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long>, JpaSpecificationExecutor<ServiceEntity> {

    long countByCategoryId(Long categoryId);

    boolean existsByCategoryId(Long categoryId);
}