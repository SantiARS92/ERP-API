package es.pedro.pymes_backend.infrastructure.web.dto;

import es.pedro.pymes_backend.domain.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class AppointmentResponse {

    private Long id;

    private Long clientId;
    private String clientName;

    private Long serviceId;
    private String serviceName;

    private Double price;

    private String professionalName;

    private LocalDate appointmentDate;
    private LocalTime appointmentTime;

    private AppointmentStatus status;

    private String internalNotes;
}