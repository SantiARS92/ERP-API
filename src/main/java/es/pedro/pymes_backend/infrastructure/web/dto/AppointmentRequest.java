package es.pedro.pymes_backend.infrastructure.web.dto;

import es.pedro.pymes_backend.domain.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class AppointmentRequest {

    @NotNull
    private Long clientId;

    @NotNull
    private Long serviceId;

    @NotNull
    @Positive
    private Double price;

    private Long professionalId;

    private String professionalName;

    @NotNull
    private LocalDate appointmentDate;

    @NotNull
    private LocalTime appointmentTime;

    @NotNull
    private AppointmentStatus status;

    private String internalNotes;
}
