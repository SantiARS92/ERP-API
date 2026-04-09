package es.pedro.pymes_backend.infrastructure.repository;

import es.pedro.pymes_backend.domain.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public interface AppointmentListProjection {

    Long getId();

    Long getClientId();

    String getClientName();

    Long getServiceId();

    String getServiceName();

    Double getPrice();

    Long getProfessionalId();

    String getProfessionalName();

    LocalDate getAppointmentDate();

    LocalTime getAppointmentTime();

    AppointmentStatus getStatus();

    String getInternalNotes();
}
