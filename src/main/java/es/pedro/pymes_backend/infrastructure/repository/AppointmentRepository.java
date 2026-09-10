package es.pedro.pymes_backend.infrastructure.repository;

import es.pedro.pymes_backend.domain.AppointmentEntity;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long> {

    @Query("""
            select
                a.id as id,
                a.clientId as clientId,
                a.clientName as clientName,
                a.serviceId as serviceId,
                a.serviceName as serviceName,
                a.price as price,
                a.professionalId as professionalId,
                a.professionalName as professionalName,
                a.appointmentDate as appointmentDate,
                a.appointmentTime as appointmentTime,
                a.status as status,
                a.internalNotes as internalNotes
            from AppointmentEntity a
            where a.appointmentDate between :fromDate and :toDate
            """)
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_READ_ONLY, value = "true"),
            @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE, value = "1000")
    })
    List<AppointmentListProjection> findByAppointmentDateBetween(
            LocalDate fromDate,
            LocalDate toDate
    );
    boolean existsByProfessionalId(Long professionalId);

}
