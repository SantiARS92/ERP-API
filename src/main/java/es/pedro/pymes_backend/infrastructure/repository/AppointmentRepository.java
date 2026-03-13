package es.pedro.pymes_backend.infrastructure.repository;

import es.pedro.pymes_backend.domain.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long> {

    List<AppointmentEntity> findByAppointmentDateBetween(LocalDate fromDate, LocalDate toDate);

}