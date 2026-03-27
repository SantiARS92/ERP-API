package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.AppointmentEntity;
import es.pedro.pymes_backend.domain.AppointmentStatus;
import es.pedro.pymes_backend.domain.ProfessionalEntity;
import es.pedro.pymes_backend.infrastructure.repository.AppointmentRepository;
import es.pedro.pymes_backend.infrastructure.repository.ClientRepository;
import es.pedro.pymes_backend.infrastructure.repository.ProfessionalRepository;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import es.pedro.pymes_backend.domain.ClientEntity;
import es.pedro.pymes_backend.domain.ServiceEntity;
import es.pedro.pymes_backend.infrastructure.web.dto.AppointmentRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.AppointmentResponse;
import es.pedro.pymes_backend.infrastructure.web.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final ServiceRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;
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
        applyProfessionalData(appointment, request);

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
        applyProfessionalData(appointment, request);

        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());

        appointment.setStatus(request.getStatus());
        appointment.setInternalNotes(request.getInternalNotes());

        AppointmentEntity updated = appointmentRepository.save(appointment);

        if (!wasConfirmedBefore && updated.getStatus() == AppointmentStatus.CONFIRMED) {
            saleService.createSaleFromConfirmedAppointment(updated);
        }
        if (wasConfirmedBefore && updated.getStatus() != AppointmentStatus.CONFIRMED) {
            saleService.deleteSaleFromAppointment(updated.getId());
        }

        return mapToResponse(updated);
    }

    public void deleteAppointment(Long id) {

        AppointmentEntity appointment = appointmentRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + id));

        saleService.deleteSaleFromAppointment(appointment.getId());
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
                appointment.getProfessionalId(),
                appointment.getProfessionalName(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getStatus(),
                appointment.getInternalNotes()
        );
    }

    private void applyProfessionalData(AppointmentEntity appointment, AppointmentRequest request) {

        if (request.getProfessionalId() == null) {
            appointment.setProfessionalId(null);
            appointment.setProfessionalName(request.getProfessionalName());
            return;
        }

        ProfessionalEntity professional = professionalRepository
                .findByIdAndActiveTrue(request.getProfessionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado o inactivo"));

        appointment.setProfessionalId(professional.getId());
        appointment.setProfessionalName(professional.getFullName());
    }
}
