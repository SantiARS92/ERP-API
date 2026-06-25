package es.supplesoftware.autonomos_api.settings.infrastructure;
import es.supplesoftware.autonomos_api.settings.infrastructure.BusinessSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessSettingsRepository extends JpaRepository<BusinessSettingsEntity, Long> {

    Optional<BusinessSettingsEntity> findFirstByOrderByIdAsc();
}