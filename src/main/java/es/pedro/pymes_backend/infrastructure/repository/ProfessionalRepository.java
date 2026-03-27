package es.pedro.pymes_backend.infrastructure.repository;

import es.pedro.pymes_backend.domain.ProfessionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfessionalRepository extends JpaRepository<ProfessionalEntity, Long> {

    List<ProfessionalEntity> findAllByOrderByFullNameAsc();

    List<ProfessionalEntity> findByActiveOrderByFullNameAsc(Boolean active);

    Optional<ProfessionalEntity> findByIdAndActiveTrue(Long id);
}