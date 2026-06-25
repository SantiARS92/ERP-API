package es.supplesoftware.autonomos_api.appointments.application;
import es.supplesoftware.autonomos_api.appointments.infrastructure.AppointmentEntity;
import es.supplesoftware.autonomos_api.appointments.domain.enums.AppointmentStatus;

import es.supplesoftware.autonomos_api.professionals.infrastructure.ProfessionalEntity;
import es.supplesoftware.autonomos_api.sales.application.SaleService;
import es.supplesoftware.autonomos_api.appointments.application.dto.AppointmentListProjection;
import es.supplesoftware.autonomos_api.appointments.infrastructure.AppointmentRepository;
import es.supplesoftware.autonomos_api.clients.infrastructure.ClientRepository;
import es.supplesoftware.autonomos_api.professionals.infrastructure.ProfessionalRepository;
import es.supplesoftware.autonomos_api.services.infrastructure.ServiceRepository;
import es.supplesoftware.autonomos_api.clients.infrastructure.ClientEntity;
import es.supplesoftware.autonomos_api.services.infrastructure.ServiceEntity;
import es.supplesoftware.autonomos_api.appointments.application.dto.AppointmentRequest;
import es.supplesoftware.autonomos_api.appointments.application.dto.AppointmentResponse;
import es.supplesoftware.autonomos_api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointments(LocalDate fromDate, LocalDate toDate) {
        return appointmentRepository
                .findByAppointmentDateBetween(fromDate, toDate)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
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

        AppointmentEntity created = appointmentRepository.save(appointment);
        syncAppointmentSale(created, false);

        return mapToResponse(created);
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
        syncAppointmentSale(updated, wasConfirmedBefore);

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

    private AppointmentResponse mapToResponse(AppointmentListProjection appointment) {
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

    private void syncAppointmentSale(AppointmentEntity appointment, boolean wasConfirmedBefore) {
        if (!wasConfirmedBefore && appointment.getStatus() == AppointmentStatus.CONFIRMED) {
            saleService.createSaleFromConfirmedAppointment(appointment);
        }
        if (wasConfirmedBefore && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            saleService.deleteSaleFromAppointment(appointment.getId());
        }
    }
}
