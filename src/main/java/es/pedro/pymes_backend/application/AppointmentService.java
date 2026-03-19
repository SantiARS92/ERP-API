package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.AppointmentEntity;
import es.pedro.pymes_backend.domain.AppointmentStatus;
import es.pedro.pymes_backend.infrastructure.repository.AppointmentRepository;
import es.pedro.pymes_backend.infrastructure.repository.ClientRepository;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import es.pedro.pymes_backend.domain.ClientEntity;
import es.pedro.pymes_backend.domain.ServiceEntity;
import es.pedro.pymes_backend.infrastructure.web.dto.AppointmentRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.AppointmentResponse;
import es.pedro.pymes_backend.infrastructure.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final ServiceRepository serviceRepository;
    private final SaleService saleService;

    public List<AppointmentResponse> getAppointments(LocalDate fromDate, LocalDate toDate) {

        return appointmentRepository
                .findByAppointmentDateBetween(fromDate, toDate)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public AppointmentResponse getAppointment(Long id) {

        AppointmentEntity appointment = appointmentRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + id));

        return mapToResponse(appointment);
    }

    public AppointmentResponse createAppointment(AppointmentRequest request) {

        ClientEntity client = clientRepository
                .findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        ServiceEntity service = serviceRepository
                .findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        AppointmentEntity appointment = new AppointmentEntity();

        appointment.setClientId(client.getId());
        appointment.setClientName(client.getFullName());

        appointment.setServiceId(service.getId());
        appointment.setServiceName(service.getName());

        appointment.setPrice(request.getPrice());
        appointment.setProfessionalName(request.getProfessionalName());

        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());

        appointment.setStatus(request.getStatus());
        appointment.setInternalNotes(request.getInternalNotes());

        return mapToResponse(appointmentRepository.save(appointment));
    }

    public AppointmentResponse updateAppointment(Long id, AppointmentRequest request) {

        AppointmentEntity appointment = appointmentRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + id));

        boolean wasConfirmedBefore = appointment.getStatus() == AppointmentStatus.CONFIRMED;

        ClientEntity client = clientRepository
                .findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        ServiceEntity service = serviceRepository
                .findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        appointment.setClientId(client.getId());
        appointment.setClientName(client.getFullName());

        appointment.setServiceId(service.getId());
        appointment.setServiceName(service.getName());

        appointment.setPrice(request.getPrice());
        appointment.setProfessionalName(request.getProfessionalName());

        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());

        appointment.setStatus(request.getStatus());
        appointment.setInternalNotes(request.getInternalNotes());

        AppointmentEntity updated = appointmentRepository.save(appointment);

        if (!wasConfirmedBefore && updated.getStatus() == AppointmentStatus.CONFIRMED) {
            saleService.createSaleFromConfirmedAppointment(updated);
        }

        return mapToResponse(updated);
    }

    public void deleteAppointment(Long id) {

        AppointmentEntity appointment = appointmentRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + id));

        appointmentRepository.delete(appointment);
    }

    private AppointmentResponse mapToResponse(AppointmentEntity appointment) {

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getClientId(),
                appointment.getClientName(),
                appointment.getServiceId(),
                appointment.getServiceName(),
                appointment.getPrice(),
                appointment.getProfessionalName(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getStatus(),
                appointment.getInternalNotes()
        );
    }
}
