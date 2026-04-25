package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.AppointmentStatus;
import es.pedro.pymes_backend.domain.CategoryEntity;
import es.pedro.pymes_backend.domain.ClientEntity;
import es.pedro.pymes_backend.domain.SaleEntity;
import es.pedro.pymes_backend.domain.SaleItemType;
import es.pedro.pymes_backend.domain.SaleSourceType;
import es.pedro.pymes_backend.domain.ServiceEntity;
import es.pedro.pymes_backend.infrastructure.repository.CategoryRepository;
import es.pedro.pymes_backend.infrastructure.repository.ClientRepository;
import es.pedro.pymes_backend.infrastructure.repository.SaleRepository;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.AppointmentRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.AppointmentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppointmentSaleIntegrationTest {

    private static final LocalDate APPOINTMENT_DATE = LocalDate.of(2026, 4, 25);
    private static final LocalTime APPOINTMENT_TIME = LocalTime.of(10, 30);

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Test
    void createConfirmedAppointmentShouldCreateAutomaticSale() {
        ClientEntity client = saveClient("Cliente cita confirmada");
        ServiceEntity service = saveService("Servicio confirmado");

        AppointmentResponse appointment = appointmentService.createAppointment(
                buildAppointmentRequest(client, service, AppointmentStatus.CONFIRMED)
        );

        List<SaleEntity> appointmentSales = findAppointmentSales(appointment.getId());

        assertThat(appointmentSales).hasSize(1);

        SaleEntity sale = appointmentSales.get(0);
        assertThat(sale.getSourceType()).isEqualTo(SaleSourceType.APPOINTMENT);
        assertThat(sale.getItemType()).isEqualTo(SaleItemType.SERVICE);
        assertThat(sale.getInventoryProduct()).isNull();
        assertThat(sale.getInventoryProductVariant()).isNull();
        assertThat(sale.getService().getId()).isEqualTo(service.getId());
        assertThat(sale.getSaleDate()).isEqualTo(APPOINTMENT_DATE);
        assertThat(sale.getQuantity()).isEqualTo(1);
        assertThat(sale.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(service.getPrice()));
    }

    @Test
    void createPendingAppointmentShouldNotCreateAutomaticSale() {
        ClientEntity client = saveClient("Cliente cita pendiente");
        ServiceEntity service = saveService("Servicio pendiente");

        AppointmentResponse appointment = appointmentService.createAppointment(
                buildAppointmentRequest(client, service, AppointmentStatus.PENDING)
        );

        assertThat(findAppointmentSales(appointment.getId())).isEmpty();
    }

    @Test
    void updateAppointmentFromPendingToConfirmedShouldCreateAutomaticSale() {
        ClientEntity client = saveClient("Cliente cita transicion");
        ServiceEntity service = saveService("Servicio transicion");

        AppointmentResponse appointment = appointmentService.createAppointment(
                buildAppointmentRequest(client, service, AppointmentStatus.PENDING)
        );

        appointmentService.updateAppointment(
                appointment.getId(),
                buildAppointmentRequest(client, service, AppointmentStatus.CONFIRMED)
        );

        List<SaleEntity> appointmentSales = findAppointmentSales(appointment.getId());

        assertThat(appointmentSales).hasSize(1);
        assertThat(appointmentSales.get(0).getSourceType()).isEqualTo(SaleSourceType.APPOINTMENT);
    }

    @Test
    void updateAlreadyConfirmedAppointmentShouldNotDuplicateAutomaticSale() {
        ClientEntity client = saveClient("Cliente cita reconfirmada");
        ServiceEntity service = saveService("Servicio reconfirmado");

        AppointmentResponse appointment = appointmentService.createAppointment(
                buildAppointmentRequest(client, service, AppointmentStatus.CONFIRMED)
        );

        SaleEntity originalSale = saleRepository.findByAppointmentId(appointment.getId()).orElseThrow();

        AppointmentRequest updatedRequest = buildAppointmentRequest(client, service, AppointmentStatus.CONFIRMED);
        updatedRequest.setAppointmentTime(APPOINTMENT_TIME.plusHours(1));
        updatedRequest.setInternalNotes("Cita confirmada editada");

        appointmentService.updateAppointment(appointment.getId(), updatedRequest);

        List<SaleEntity> appointmentSales = findAppointmentSales(appointment.getId());

        assertThat(appointmentSales).hasSize(1);
        assertThat(appointmentSales.get(0).getId()).isEqualTo(originalSale.getId());
    }

    @Test
    void updateAppointmentToConfirmedShouldNotDuplicateExistingAppointmentSale() {
        ClientEntity client = saveClient("Cliente cita venta previa");
        ServiceEntity service = saveService("Servicio venta previa");

        AppointmentResponse appointment = appointmentService.createAppointment(
                buildAppointmentRequest(client, service, AppointmentStatus.PENDING)
        );

        SaleEntity existingSale = saveAppointmentSale(appointment.getId(), client, service);

        appointmentService.updateAppointment(
                appointment.getId(),
                buildAppointmentRequest(client, service, AppointmentStatus.CONFIRMED)
        );

        List<SaleEntity> appointmentSales = findAppointmentSales(appointment.getId());

        assertThat(appointmentSales).hasSize(1);
        assertThat(appointmentSales.get(0).getId()).isEqualTo(existingSale.getId());
    }

    private AppointmentRequest buildAppointmentRequest(
            ClientEntity client,
            ServiceEntity service,
            AppointmentStatus status
    ) {
        AppointmentRequest request = new AppointmentRequest();
        request.setClientId(client.getId());
        request.setServiceId(service.getId());
        request.setPrice(service.getPrice());
        request.setProfessionalName("Laura");
        request.setAppointmentDate(APPOINTMENT_DATE);
        request.setAppointmentTime(APPOINTMENT_TIME);
        request.setStatus(status);
        request.setInternalNotes("Appointment sale integration");
        return request;
    }

    private List<SaleEntity> findAppointmentSales(Long appointmentId) {
        return saleRepository.findAll().stream()
                .filter(sale -> appointmentId.equals(sale.getAppointmentId()))
                .toList();
    }

    private SaleEntity saveAppointmentSale(Long appointmentId, ClientEntity client, ServiceEntity service) {
        SaleEntity sale = new SaleEntity();
        sale.setSourceType(SaleSourceType.APPOINTMENT);
        sale.setAppointmentId(appointmentId);
        sale.setClient(client);
        sale.setClientName(client.getFullName());
        sale.setItemType(SaleItemType.SERVICE);
        sale.setService(service);
        sale.setInventoryProduct(null);
        sale.setInventoryProductVariant(null);
        sale.setItemName(service.getName());
        sale.setUnitPrice(BigDecimal.valueOf(service.getPrice()));
        sale.setQuantity(1);
        sale.setTotalAmount(BigDecimal.valueOf(service.getPrice()));
        sale.setPaymentMethod(null);
        sale.setSaleDate(APPOINTMENT_DATE);
        sale.setNotes("Existing appointment sale");
        return saleRepository.saveAndFlush(sale);
    }

    private ClientEntity saveClient(String fullNamePrefix) {
        ClientEntity client = new ClientEntity();
        client.setFullName(fullNamePrefix + " " + UUID.randomUUID());
        client.setPhone("600000000");
        client.setEmail(UUID.randomUUID() + "@test.com");
        return clientRepository.saveAndFlush(client);
    }

    private ServiceEntity saveService(String namePrefix) {
        CategoryEntity category = categoryRepository.saveAndFlush(
                CategoryEntity.builder()
                        .name(namePrefix + " " + UUID.randomUUID())
                        .color("blue")
                        .icon("tag")
                        .build()
        );

        return serviceRepository.saveAndFlush(
                ServiceEntity.builder()
                        .name(namePrefix + " " + UUID.randomUUID())
                        .description("Appointment sale integration service")
                        .durationMinutes(45)
                        .price(35.0)
                        .active(true)
                        .category(category)
                        .build()
        );
    }
}
