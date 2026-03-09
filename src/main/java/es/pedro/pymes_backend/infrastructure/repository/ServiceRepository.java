package es.pedro.pymes_backend.infrastructure.repository;

import es.pedro.pymes_backend.domain.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
}