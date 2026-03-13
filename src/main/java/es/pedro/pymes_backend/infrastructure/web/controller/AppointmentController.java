package es.pedro.pymes_backend.infrastructure.web.controller;

import es.pedro.pymes_backend.application.AppointmentService;
import es.pedro.pymes_backend.infrastructure.web.dto.AppointmentRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.AppointmentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public List<AppointmentResponse> getAppointments(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate
    ) {
        return appointmentService.getAppointments(fromDate, toDate);
    }

    @GetMapping("/{id}")
    public AppointmentResponse getAppointment(@PathVariable Long id) {
        return appointmentService.getAppointment(id);
    }

    @PostMapping
    public AppointmentResponse createAppointment(@Valid @RequestBody AppointmentRequest request) {
        return appointmentService.createAppointment(request);
    }

    @PutMapping("/{id}")
    public AppointmentResponse updateAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRequest request
    ) {
        return appointmentService.updateAppointment(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteAppointment(@PathVariable Long id) {
        appointmentService.deleteAppointment(id);
    }
}