package es.pedro.pymes_backend.infrastructure.repository;

import es.pedro.pymes_backend.domain.BusinessSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessSettingsRepository extends JpaRepository<BusinessSettingsEntity, Long> {

    Optional<BusinessSettingsEntity> findFirstByOrderByIdAsc();
}