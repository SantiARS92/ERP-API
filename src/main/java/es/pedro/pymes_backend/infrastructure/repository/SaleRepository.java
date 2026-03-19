package es.pedro.pymes_backend.infrastructure.repository;

import es.pedro.pymes_backend.domain.SaleEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<SaleEntity, Long>, JpaSpecificationExecutor<SaleEntity> {

    boolean existsByAppointmentId(Long appointmentId);

    Optional<SaleEntity> findByAppointmentId(Long appointmentId);

    List<SaleEntity> findBySaleDateGreaterThanEqualOrderBySaleDateDescSaleTimeDesc(
            java.time.LocalDate date, Pageable pageable
    );
}