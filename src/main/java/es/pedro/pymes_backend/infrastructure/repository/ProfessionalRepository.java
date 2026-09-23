package es.pedro.pymes_backend.infrastructure.repository;

import es.pedro.pymes_backend.domain.ProfessionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfessionalRepository extends JpaRepository<ProfessionalEntity, Long> {

    List<ProfessionalEntity> findAllByOrderByFullNameAsc();

    List<ProfessionalEntity> findByActiveOrderByFullNameAsc(Boolean active);

    Optional<ProfessionalEntity> findByIdAndActiveTrue(Long id);

    @Query("""
            select p
            from ProfessionalEntity p
            where lower(trim(p.fullName)) = lower(trim(:fullName))
            """)
    Optional<ProfessionalEntity> findByNormalizedFullName(@Param("fullName") String fullName);

    @Query ("""
            select p
            from ProfessionalEntity p
            where lower(trim(p.email)) = lower(trim(:email))
            """)
    Optional<ProfessionalEntity> findByNormalizedEmail(@Param("email") String email);
}
