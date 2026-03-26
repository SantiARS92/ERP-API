package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.AppointmentEntity;
import es.pedro.pymes_backend.domain.AppointmentStatus;
import es.pedro.pymes_backend.domain.CategoryEntity;
import es.pedro.pymes_backend.domain.ClientEntity;
import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.domain.SaleEntity;
import es.pedro.pymes_backend.domain.SaleItemType;
import es.pedro.pymes_backend.domain.SaleSourceType;
import es.pedro.pymes_backend.domain.ServiceEntity;
import es.pedro.pymes_backend.infrastructure.repository.CategoryRepository;
import es.pedro.pymes_backend.infrastructure.repository.ClientRepository;
import es.pedro.pymes_backend.infrastructure.repository.InventoryProductRepository;
import es.pedro.pymes_backend.infrastructure.repository.SaleRepository;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateSaleRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.SaleResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.UpdateSaleRequest;
import es.pedro.pymes_backend.infrastructure.web.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SaleInventoryIntegrationTest {

    private static final LocalDate SALE_DATE = LocalDate.of(2026, 3, 26);
    private static final LocalTime SALE_TIME = LocalTime.of(10, 30);
    private static final String SHAMPOO_NAME = "Champ\u00fa L'Oreal Prof";

    @Autowired
    private SaleService saleService;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private InventoryProductRepository inventoryProductRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Test
    void createManualProductSaleShouldDiscountStockUsingInventoryProductId() {
        ClientEntity client = saveClient("Cliente producto");
        InventoryProductEntity product = saveProduct(SHAMPOO_NAME, 20);

        SaleResponse sale = saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductId(product.getId())
                        .itemName("Ignorado")
                        .unitPrice(new BigDecimal("12.50"))
                        .quantity(2)
                        .saleDate(SALE_DATE)
                        .saleTime(SALE_TIME)
                        .notes("Venta de producto")
                        .build()
        );

        assertThat(sale.getInventoryProductId()).isEqualTo(product.getId());
        assertThat(sale.getItemName()).isEqualTo(product.getName());
        assertThat(getStock(product.getId())).isEqualTo(18);
        assertThat(getSale(sale.getId()).getInventoryProduct()).isNotNull();
    }

    @Test
    void createManualProductSaleShouldReturnConflictWhenQuantityExceedsStock() {
        ClientEntity client = saveClient("Cliente sin stock");
        InventoryProductEntity product = saveProduct(SHAMPOO_NAME, 18);

        assertThatThrownBy(() -> saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductId(product.getId())
                        .unitPrice(new BigDecimal("12.50"))
                        .quantity(50)
                        .saleDate(SALE_DATE)
                        .saleTime(SALE_TIME)
                        .build()
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("No hay stock suficiente para el producto con id: " + product.getId());

        assertThat(getStock(product.getId())).isEqualTo(18);
        assertThat(saleRepository.count()).isZero();
    }

    @Test
    void createManualServiceSaleShouldNotTouchInventory() {
        ClientEntity client = saveClient("Cliente servicio");
        InventoryProductEntity product = saveProduct("Mascarilla", 9);
        ServiceEntity service = saveService("Corte premium", 25.0);

        SaleResponse sale = saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.SERVICE)
                        .serviceId(service.getId())
                        .unitPrice(new BigDecimal("25.00"))
                        .quantity(1)
                        .saleDate(SALE_DATE)
                        .saleTime(SALE_TIME)
                        .notes("Venta de servicio")
                        .build()
        );

        assertThat(sale.getInventoryProductId()).isNull();
        assertThat(sale.getServiceId()).isEqualTo(service.getId());
        assertThat(sale.getItemName()).isEqualTo(service.getName());
        assertThat(getStock(product.getId())).isEqualTo(9);
    }

    @Test
    void updateProductSaleShouldRestorePreviousStockAndDiscountNewStock() {
        ClientEntity client = saveClient("Cliente update");
        InventoryProductEntity product = saveProduct(SHAMPOO_NAME, 20);

        SaleResponse createdSale = saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductId(product.getId())
                        .unitPrice(new BigDecimal("11.00"))
                        .quantity(2)
                        .saleDate(SALE_DATE)
                        .saleTime(SALE_TIME)
                        .build()
        );

        SaleResponse updatedSale = saleService.updateSale(
                createdSale.getId(),
                UpdateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductId(product.getId())
                        .unitPrice(new BigDecimal("14.00"))
                        .quantity(4)
                        .saleDate(SALE_DATE)
                        .saleTime(SALE_TIME.plusHours(1))
                        .build()
        );

        assertThat(updatedSale.getInventoryProductId()).isEqualTo(product.getId());
        assertThat(updatedSale.getItemName()).isEqualTo(product.getName());
        assertThat(getStock(product.getId())).isEqualTo(16);
    }

    @Test
    void deleteManualProductSaleShouldRestoreStock() {
        ClientEntity client = saveClient("Cliente delete");
        InventoryProductEntity product = saveProduct("Serum", 12);

        SaleResponse sale = saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductId(product.getId())
                        .unitPrice(new BigDecimal("8.00"))
                        .quantity(4)
                        .saleDate(SALE_DATE)
                        .saleTime(SALE_TIME)
                        .build()
        );

        saleService.deleteSale(sale.getId());

        assertThat(getStock(product.getId())).isEqualTo(12);
        assertThat(saleRepository.findById(sale.getId())).isEmpty();
    }

    @Test
    void appointmentSaleShouldRemainServiceAndDeleteShouldKeepConflictWithoutTouchingInventory() {
        ClientEntity client = saveClient("Cliente cita");
        InventoryProductEntity product = saveProduct("Espuma", 7);
        ServiceEntity service = saveService("Limpieza facial", 30.0);

        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setId(99L);
        appointment.setClientId(client.getId());
        appointment.setClientName(client.getFullName());
        appointment.setServiceId(service.getId());
        appointment.setServiceName(service.getName());
        appointment.setPrice(service.getPrice());
        appointment.setAppointmentDate(SALE_DATE);
        appointment.setAppointmentTime(SALE_TIME);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setInternalNotes("Auto sale");

        saleService.createSaleFromConfirmedAppointment(appointment);

        SaleEntity appointmentSale = saleRepository.findByAppointmentId(appointment.getId())
                .orElseThrow();

        assertThat(appointmentSale.getSourceType()).isEqualTo(SaleSourceType.APPOINTMENT);
        assertThat(appointmentSale.getItemType()).isEqualTo(SaleItemType.SERVICE);
        assertThat(appointmentSale.getInventoryProduct()).isNull();
        assertThat(getStock(product.getId())).isEqualTo(7);

        assertThatThrownBy(() -> saleService.deleteSale(appointmentSale.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("No se puede borrar una venta generada desde una cita");

        assertThat(getStock(product.getId())).isEqualTo(7);
    }

    private ClientEntity saveClient(String name) {
        ClientEntity client = new ClientEntity();
        client.setFullName(name);
        client.setPhone("600000000");
        client.setEmail(name.replace(" ", ".").toLowerCase() + "@test.com");
        return clientRepository.saveAndFlush(client);
    }

    private InventoryProductEntity saveProduct(String name, int stock) {
        CategoryEntity category = saveCategory();

        return inventoryProductRepository.saveAndFlush(
                InventoryProductEntity.builder()
                        .name(name)
                        .description("Producto test")
                        .category(category)
                        .stock(stock)
                        .minimumStock(2)
                        .sku("SKU-" + UUID.randomUUID())
                        .purchasePrice(new BigDecimal("5.00"))
                        .active(true)
                        .build()
        );
    }

    private ServiceEntity saveService(String name, double price) {
        CategoryEntity category = saveCategory();

        return serviceRepository.saveAndFlush(
                ServiceEntity.builder()
                        .name(name)
                        .description("Servicio test")
                        .durationMinutes(60)
                        .price(price)
                        .active(true)
                        .category(category)
                        .build()
        );
    }

    private CategoryEntity saveCategory() {
        return categoryRepository.saveAndFlush(
                CategoryEntity.builder()
                        .name("Category-" + UUID.randomUUID())
                        .color("blue")
                        .icon("tag")
                        .build()
        );
    }

    private int getStock(Long productId) {
        return inventoryProductRepository.findById(productId)
                .orElseThrow()
                .getStock();
    }

    private SaleEntity getSale(Long saleId) {
        return saleRepository.findById(saleId)
                .orElseThrow();
    }
}
