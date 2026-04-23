package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.AppointmentEntity;
import es.pedro.pymes_backend.domain.AppointmentStatus;
import es.pedro.pymes_backend.domain.ClientEntity;
import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.domain.InventoryProductVariantEntity;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SaleServiceImpl implements SaleService {

    private static final String WALK_IN_CLIENT_NAME = "Al contado";

    private final SaleRepository saleRepository;
    private final ClientRepository clientRepository;
    private final ServiceRepository serviceRepository;
    private final InventoryProductService inventoryProductService;

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
                request.getInventoryProductId(),
                request.getInventoryProductVariantId(),
                request.getItemName(),
                request.getUnitPrice(),
                request.getQuantity(),
                request.getPaymentMethod(),
                request.getSaleDate(),
                request.getNotes()
        );

        return toResponse(saleRepository.save(sale));
    }

    @Override
    public SaleResponse updateSale(Long id, UpdateSaleRequest request) {
        SaleEntity sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));

        restoreInventoryStockIfNeeded(sale);

        ClientEntity client = resolveClient(request.getClientId());
        sale.setClient(client);
        sale.setClientName(client.getFullName());

        applySaleData(
                sale,
                request.getItemType(),
                request.getServiceId(),
                request.getInventoryProductId(),
                request.getInventoryProductVariantId(),
                request.getItemName(),
                request.getUnitPrice(),
                request.getQuantity(),
                request.getPaymentMethod(),
                request.getSaleDate(),
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

        restoreInventoryStockIfNeeded(sale);

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
                        Sort.Order.desc("id")
                )
        );

        return saleRepository.findBySaleDateGreaterThanEqualOrderBySaleDateDescIdDesc(fromDate, pageable)
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
        sale.setInventoryProduct(null);
        sale.setInventoryProductVariant(null);
        sale.setItemName(appointment.getServiceName());
        sale.setUnitPrice(unitPrice);
        sale.setQuantity(1);
        sale.setTotalAmount(unitPrice);
        sale.setPaymentMethod(null);
        sale.setSaleDate(appointment.getAppointmentDate());
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
            Long inventoryProductId,
            Long inventoryProductVariantId,
            String itemName,
            BigDecimal unitPrice,
            Integer quantity,
            PaymentMethod paymentMethod,
            LocalDate saleDate,
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

        sale.setItemType(itemType);

        if (itemType == SaleItemType.SERVICE) {
            validateServiceSaleReferences(inventoryProductId, inventoryProductVariantId);
            ServiceEntity service = resolveService(serviceId);
            BigDecimal resolvedUnitPrice = unitPrice != null ? unitPrice : BigDecimal.valueOf(service.getPrice());

            validateUnitPrice(resolvedUnitPrice);

            sale.setService(service);
            sale.setInventoryProduct(null);
            sale.setInventoryProductVariant(null);
            sale.setItemName(service.getName());
            sale.setUnitPrice(resolvedUnitPrice);
        } else {
            if (unitPrice == null) {
                throw badRequest("unitPrice es obligatorio para ventas de producto");
            }

            validateUnitPrice(unitPrice);

            sale.setService(null);
            sale.setUnitPrice(unitPrice);
            applyProductSaleData(sale, inventoryProductId, inventoryProductVariantId, quantity);
        }

        sale.setQuantity(quantity);
        sale.setTotalAmount(calculateTotal(sale.getUnitPrice(), quantity));
        sale.setPaymentMethod(paymentMethod);
        sale.setSaleDate(saleDate);
        sale.setNotes(normalizeText(notes));
    }

    private ServiceEntity resolveService(Long serviceId) {
        if (serviceId == null) {
            throw badRequest("serviceId es obligatorio para ventas de servicio");
        }

        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + serviceId));
    }

    private InventoryProductEntity resolveInventoryProduct(Long inventoryProductId) {
        if (inventoryProductId == null) {
            throw badRequest("inventoryProductId es obligatorio para ventas de producto");
        }

        return inventoryProductService.getActiveProductEntityById(inventoryProductId);
    }

    private InventoryProductVariantEntity resolveInventoryProductVariant(Long inventoryProductVariantId) {
        if (inventoryProductVariantId == null) {
            throw badRequest("inventoryProductVariantId es obligatorio para ventas de producto con variantes");
        }

        return inventoryProductService.getActiveProductVariantEntityById(inventoryProductVariantId);
    }

    private void validateServiceSaleReferences(Long inventoryProductId, Long inventoryProductVariantId) {
        if (inventoryProductId != null || inventoryProductVariantId != null) {
            throw badRequest("Las ventas de servicio no pueden incluir producto ni variante de inventario");
        }
    }

    private void applyProductSaleData(
            SaleEntity sale,
            Long inventoryProductId,
            Long inventoryProductVariantId,
            Integer quantity
    ) {
        if (inventoryProductVariantId != null) {
            InventoryProductVariantEntity variant = resolveInventoryProductVariant(inventoryProductVariantId);
            InventoryProductEntity product = variant.getProduct();

            if (!Boolean.TRUE.equals(product.getHasVariants())) {
                throw badRequest("La variante seleccionada no pertenece a un producto con variantes");
            }
            if (inventoryProductId != null && !inventoryProductId.equals(product.getId())) {
                throw badRequest("inventoryProductId no coincide con el producto base de la variante");
            }

            InventoryProductVariantEntity updatedVariant = inventoryProductService.subtractVariantStock(variant, quantity);
            sale.setInventoryProduct(updatedVariant.getProduct());
            sale.setInventoryProductVariant(updatedVariant);
            sale.setItemName(buildVariantItemName(updatedVariant));
            return;
        }

        if (inventoryProductId == null) {
            throw badRequest("inventoryProductId o inventoryProductVariantId es obligatorio para ventas de producto");
        }

        InventoryProductEntity inventoryProduct = resolveInventoryProduct(inventoryProductId);
        if (Boolean.TRUE.equals(inventoryProduct.getHasVariants())) {
            throw badRequest("inventoryProductVariantId es obligatorio para vender un producto con variantes");
        }

        InventoryProductEntity updatedInventoryProduct = inventoryProductService.subtractStock(inventoryProduct, quantity);
        sale.setInventoryProduct(updatedInventoryProduct);
        sale.setInventoryProductVariant(null);
        sale.setItemName(updatedInventoryProduct.getName());
    }

    private String buildVariantItemName(InventoryProductVariantEntity variant) {
        return variant.getProduct().getName() + " - " + variant.getVariantLabel();
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

    private void restoreInventoryStockIfNeeded(SaleEntity sale) {
        if (sale.getInventoryProductVariant() != null) {
            inventoryProductService.addVariantStock(sale.getInventoryProductVariant(), sale.getQuantity());
            sale.setInventoryProductVariant(null);
            sale.setInventoryProduct(null);
            return;
        }

        if (sale.getInventoryProduct() == null) {
            return;
        }

        inventoryProductService.addStock(sale.getInventoryProduct(), sale.getQuantity());
        sale.setInventoryProduct(null);
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
                .inventoryProductId(sale.getInventoryProduct() != null ? sale.getInventoryProduct().getId() : null)
                .inventoryProductVariantId(sale.getInventoryProductVariant() != null ? sale.getInventoryProductVariant().getId() : null)
                .inventoryProductVariantLabel(sale.getInventoryProductVariant() != null ? sale.getInventoryProductVariant().getVariantLabel() : null)
                .inventoryProductVariantSku(sale.getInventoryProductVariant() != null ? sale.getInventoryProductVariant().getSku() : null)
                .itemName(sale.getItemName())
                .unitPrice(sale.getUnitPrice())
                .quantity(sale.getQuantity())
                .totalAmount(sale.getTotalAmount())
                .paymentMethod(sale.getPaymentMethod())
                .saleDate(sale.getSaleDate())
                .notes(sale.getNotes())
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt())
                .build();
    }
}
