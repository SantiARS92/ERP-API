package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.AppointmentEntity;
import es.pedro.pymes_backend.domain.AppointmentStatus;
import es.pedro.pymes_backend.domain.ClientEntity;
import es.pedro.pymes_backend.domain.PaymentMethod;
import es.pedro.pymes_backend.domain.SaleEntity;
import es.pedro.pymes_backend.domain.SaleItemType;
import es.pedro.pymes_backend.domain.SaleSourceType;
import es.pedro.pymes_backend.domain.ServiceEntity;
import es.pedro.pymes_backend.infrastructure.repository.ClientRepository;
import es.pedro.pymes_backend.infrastructure.repository.SaleRepository;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateSaleRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.PagedResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.SaleResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.UpdateSaleRequest;
import es.pedro.pymes_backend.infrastructure.web.exception.ConflictException;
import es.pedro.pymes_backend.infrastructure.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SaleServiceImpl implements SaleService {

    private static final String WALK_IN_CLIENT_NAME = "Al contado";

    private final SaleRepository saleRepository;
    private final ClientRepository clientRepository;
    private final ServiceRepository serviceRepository;

    @Override
    public SaleResponse createManualSale(CreateSaleRequest request) {
        ClientEntity client = resolveClient(request.getClientId());

        SaleEntity sale = new SaleEntity();
        sale.setSourceType(SaleSourceType.MANUAL);
        sale.setAppointmentId(null);
        sale.setClient(client);
        sale.setClientName(client.getFullName());

        applySaleData(
                sale,
                request.getItemType(),
                request.getServiceId(),
                request.getItemName(),
                request.getUnitPrice(),
                request.getQuantity(),
                request.getPaymentMethod(),
                request.getSaleDate(),
                request.getSaleTime(),
                request.getNotes()
        );

        return toResponse(saleRepository.save(sale));
    }

    @Override
    public SaleResponse updateSale(Long id, UpdateSaleRequest request) {
        SaleEntity sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));

        ClientEntity client = resolveClient(request.getClientId());
        sale.setClient(client);
        sale.setClientName(client.getFullName());

        applySaleData(
                sale,
                request.getItemType(),
                request.getServiceId(),
                request.getItemName(),
                request.getUnitPrice(),
                request.getQuantity(),
                request.getPaymentMethod(),
                request.getSaleDate(),
                request.getSaleTime(),
                request.getNotes()
        );

        return toResponse(saleRepository.save(sale));
    }

    @Override
    public void deleteSale(Long id) {
        SaleEntity sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));

        if (sale.getSourceType() == SaleSourceType.APPOINTMENT) {
            throw new ConflictException("No se puede borrar una venta generada desde una cita");
        }

        saleRepository.delete(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponse getSaleById(Long id) {
        SaleEntity sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));
        return toResponse(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SaleResponse> getSales(String search, LocalDate fromDate, LocalDate toDate, int page, int size) {
        if (page < 0) {
            throw badRequest("La pagina no puede ser negativa");
        }
        if (size <= 0) {
            throw badRequest("El tamano de pagina debe ser mayor que 0");
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw badRequest("fromDate no puede ser posterior a toDate");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("saleDate"),
                        Sort.Order.desc("saleTime"),
                        Sort.Order.desc("id")
                )
        );

        Specification<SaleEntity> specification = Specification
                .where(SaleSpecification.hasSearch(search))
                .and(SaleSpecification.saleDateFrom(fromDate))
                .and(SaleSpecification.saleDateTo(toDate));

        Page<SaleEntity> result = saleRepository.findAll(specification, pageable);
        List<SaleResponse> content = result.getContent().stream().map(this::toResponse).toList();

        return new PagedResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleResponse> getRecentSales(int days, int limit) {
        if (days <= 0) {
            throw badRequest("days debe ser mayor que 0");
        }
        if (limit <= 0) {
            throw badRequest("limit debe ser mayor que 0");
        }

        LocalDate fromDate = LocalDate.now().minusDays(days - 1L);
        Pageable pageable = PageRequest.of(
                0,
                limit,
                Sort.by(
                        Sort.Order.desc("saleDate"),
                        Sort.Order.desc("saleTime")
                )
        );

        return saleRepository.findBySaleDateGreaterThanEqualOrderBySaleDateDescSaleTimeDesc(fromDate, pageable)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void createSaleFromConfirmedAppointment(AppointmentEntity appointment) {
        if (appointment == null) {
            throw badRequest("La cita no puede ser null");
        }
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            return;
        }
        if (saleRepository.existsByAppointmentId(appointment.getId())) {
            return;
        }

        ClientEntity client = clientRepository.findById(appointment.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente no encontrado con id: " + appointment.getClientId()
                ));

        ServiceEntity service = serviceRepository.findById(appointment.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Servicio no encontrado con id: " + appointment.getServiceId()
                ));

        BigDecimal unitPrice = BigDecimal.valueOf(appointment.getPrice());

        SaleEntity sale = new SaleEntity();
        sale.setSourceType(SaleSourceType.APPOINTMENT);
        sale.setAppointmentId(appointment.getId());
        sale.setClient(client);
        sale.setClientName(appointment.getClientName());
        sale.setItemType(SaleItemType.SERVICE);
        sale.setService(service);
        sale.setItemName(appointment.getServiceName());
        sale.setUnitPrice(unitPrice);
        sale.setQuantity(1);
        sale.setTotalAmount(unitPrice);
        sale.setPaymentMethod(null);
        sale.setSaleDate(appointment.getAppointmentDate());
        sale.setSaleTime(appointment.getAppointmentTime());
        sale.setNotes(normalizeText(appointment.getInternalNotes()));

        saleRepository.save(sale);
    }

    @Override
    public void deleteSaleFromAppointment(Long appointmentId) {
        if (appointmentId == null) {
            throw badRequest("El id de la cita es obligatorio");
        }

        saleRepository.findByAppointmentId(appointmentId)
                .ifPresent(saleRepository::delete);
    }

    private void applySaleData(
            SaleEntity sale,
            SaleItemType itemType,
            Long serviceId,
            String itemName,
            BigDecimal unitPrice,
            Integer quantity,
            PaymentMethod paymentMethod,
            LocalDate saleDate,
            LocalTime saleTime,
            String notes
    ) {
        if (itemType == null) {
            throw badRequest("El tipo de concepto es obligatorio");
        }
        if (quantity == null || quantity < 1) {
            throw badRequest("La cantidad debe ser al menos 1");
        }
        if (saleDate == null) {
            throw badRequest("La fecha de la venta es obligatoria");
        }
        if (saleTime == null) {
            throw badRequest("La hora de la venta es obligatoria");
        }

        sale.setItemType(itemType);

        if (itemType == SaleItemType.SERVICE) {
            ServiceEntity service = resolveService(serviceId);
            BigDecimal resolvedUnitPrice = unitPrice != null ? unitPrice : BigDecimal.valueOf(service.getPrice());

            validateUnitPrice(resolvedUnitPrice);

            sale.setService(service);
            sale.setItemName(service.getName());
            sale.setUnitPrice(resolvedUnitPrice);
        } else {
            if (itemName == null || itemName.isBlank()) {
                throw badRequest("itemName es obligatorio para ventas de producto");
            }
            if (unitPrice == null) {
                throw badRequest("unitPrice es obligatorio para ventas de producto");
            }

            validateUnitPrice(unitPrice);

            sale.setService(null);
            sale.setItemName(itemName.trim());
            sale.setUnitPrice(unitPrice);
        }

        sale.setQuantity(quantity);
        sale.setTotalAmount(calculateTotal(sale.getUnitPrice(), quantity));
        sale.setPaymentMethod(paymentMethod);
        sale.setSaleDate(saleDate);
        sale.setSaleTime(saleTime);
        sale.setNotes(normalizeText(notes));
    }

    private ServiceEntity resolveService(Long serviceId) {
        if (serviceId == null) {
            throw badRequest("serviceId es obligatorio para ventas de servicio");
        }

        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + serviceId));
    }

    private ClientEntity resolveClient(Long clientId) {
        if (clientId != null) {
            return clientRepository.findById(clientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + clientId));
        }

        return clientRepository.findByFullNameIgnoreCase(WALK_IN_CLIENT_NAME)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el cliente base 'Al contado'. Debes crearlo primero."
                ));
    }

    private void validateUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null) {
            throw badRequest("El precio unitario es obligatorio");
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw badRequest("El precio unitario no puede ser negativo");
        }
    }

    private BigDecimal calculateTotal(BigDecimal unitPrice, Integer quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity.longValue()));
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private SaleResponse toResponse(SaleEntity sale) {
        return SaleResponse.builder()
                .id(sale.getId())
                .sourceType(sale.getSourceType())
                .appointmentId(sale.getAppointmentId())
                .clientId(sale.getClient().getId())
                .clientName(sale.getClientName())
                .itemType(sale.getItemType())
                .serviceId(sale.getService() != null ? sale.getService().getId() : null)
                .itemName(sale.getItemName())
                .unitPrice(sale.getUnitPrice())
                .quantity(sale.getQuantity())
                .totalAmount(sale.getTotalAmount())
                .paymentMethod(sale.getPaymentMethod())
                .saleDate(sale.getSaleDate())
                .saleTime(sale.getSaleTime())
                .notes(sale.getNotes())
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt())
                .build();
    }
}
