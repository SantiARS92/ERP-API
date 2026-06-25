package es.supplesoftware.autonomos_api.sales;

import es.supplesoftware.autonomos_api.appointments.infrastructure.AppointmentEntity;
import es.supplesoftware.autonomos_api.appointments.domain.enums.AppointmentStatus;

import es.supplesoftware.autonomos_api.categories.infrastructure.CategoryEntity;
import es.supplesoftware.autonomos_api.clients.infrastructure.ClientEntity;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductEntity;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductVariantEntity;
import es.supplesoftware.autonomos_api.sales.infrastructure.SaleEntity;
import es.supplesoftware.autonomos_api.sales.domain.enums.SaleItemType;

import es.supplesoftware.autonomos_api.sales.domain.enums.SaleSourceType;

import es.supplesoftware.autonomos_api.services.infrastructure.ServiceEntity;
import es.supplesoftware.autonomos_api.categories.infrastructure.CategoryRepository;
import es.supplesoftware.autonomos_api.clients.infrastructure.ClientRepository;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductRepository;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductVariantRepository;
import es.supplesoftware.autonomos_api.sales.infrastructure.SaleRepository;
import es.supplesoftware.autonomos_api.services.infrastructure.ServiceRepository;
import es.supplesoftware.autonomos_api.sales.application.dto.CreateSaleRequest;
import es.supplesoftware.autonomos_api.sales.application.dto.SaleResponse;
import es.supplesoftware.autonomos_api.sales.application.dto.UpdateSaleRequest;
import es.supplesoftware.autonomos_api.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import es.supplesoftware.autonomos_api.sales.application.SaleService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SaleInventoryIntegrationTest {

    private static final LocalDate SALE_DATE = LocalDate.of(2026, 3, 26);
    private static final LocalTime APPOINTMENT_TIME = LocalTime.of(10, 30);
    private static final String SHAMPOO_NAME = "Champ\u00fa L'Oreal Prof";

    @Autowired
    private SaleService saleService;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private InventoryProductRepository inventoryProductRepository;

    @Autowired
    private InventoryProductVariantRepository inventoryProductVariantRepository;

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
                        .build()
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("No hay stock suficiente para el producto con id: " + product.getId());

        assertThat(getStock(product.getId())).isEqualTo(18);
        assertThat(saleRepository.count()).isZero();
    }

    @Test
    void createManualVariantProductSaleShouldDiscountVariantStockAndKeepBaseProductLink() {
        ClientEntity client = saveClient("Cliente variante");
        InventoryProductEntity product = saveVariantProduct("Camiseta Basica");
        InventoryProductVariantEntity variant = saveVariant(product, "Roja / M", "CAM-RO-M", 8);

        SaleResponse sale = saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductId(product.getId())
                        .inventoryProductVariantId(variant.getId())
                        .unitPrice(new BigDecimal("19.90"))
                        .quantity(3)
                        .saleDate(SALE_DATE)
                        .build()
        );

        assertThat(sale.getInventoryProductId()).isEqualTo(product.getId());
        assertThat(sale.getInventoryProductVariantId()).isEqualTo(variant.getId());
        assertThat(sale.getInventoryProductVariantLabel()).isEqualTo("Roja / M");
        assertThat(sale.getInventoryProductVariantSku()).isEqualTo("CAM-RO-M");
        assertThat(sale.getItemName()).isEqualTo("Camiseta Basica - Roja / M");
        assertThat(getStock(product.getId())).isZero();
        assertThat(getVariantStock(variant.getId())).isEqualTo(5);

        SaleEntity persistedSale = getSale(sale.getId());
        assertThat(persistedSale.getInventoryProduct()).isNotNull();
        assertThat(persistedSale.getInventoryProduct().getId()).isEqualTo(product.getId());
        assertThat(persistedSale.getInventoryProductVariant()).isNotNull();
        assertThat(persistedSale.getInventoryProductVariant().getId()).isEqualTo(variant.getId());
    }

    @Test
    void createManualVariantProductSaleCanDeriveBaseProductFromVariant() {
        ClientEntity client = saveClient("Cliente variante derivada");
        InventoryProductEntity product = saveVariantProduct("Tornillo Galvanizado");
        InventoryProductVariantEntity variant = saveVariant(product, "4mm / 20mm", "TOR-4-20", 6);

        SaleResponse sale = saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductVariantId(variant.getId())
                        .unitPrice(new BigDecimal("1.50"))
                        .quantity(2)
                        .saleDate(SALE_DATE)
                        .build()
        );

        assertThat(sale.getInventoryProductId()).isEqualTo(product.getId());
        assertThat(sale.getInventoryProductVariantId()).isEqualTo(variant.getId());
        assertThat(sale.getItemName()).isEqualTo("Tornillo Galvanizado - 4mm / 20mm");
        assertThat(getVariantStock(variant.getId())).isEqualTo(4);
    }

    @Test
    void createManualProductSaleShouldRequireVariantWhenProductHasVariants() {
        ClientEntity client = saveClient("Cliente sin variante");
        InventoryProductEntity product = saveVariantProduct("Camiseta Obligatoria");
        saveVariant(product, "Azul / L", "CAM-AZ-L", 5);

        assertThatThrownBy(() -> saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductId(product.getId())
                        .unitPrice(new BigDecimal("19.90"))
                        .quantity(1)
                        .saleDate(SALE_DATE)
                        .build()
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inventoryProductVariantId es obligatorio");

        assertThat(getVariantStockByProduct(product.getId())).isEqualTo(5);
    }

    @Test
    void createManualVariantProductSaleShouldRejectMismatchedProductId() {
        ClientEntity client = saveClient("Cliente variante producto distinto");
        InventoryProductEntity product = saveVariantProduct("Camiseta Correcta");
        InventoryProductEntity otherProduct = saveProduct("Producto Incorrecto", 5);
        InventoryProductVariantEntity variant = saveVariant(product, "Roja / L", "CAM-RO-L", 5);

        assertThatThrownBy(() -> saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductId(otherProduct.getId())
                        .inventoryProductVariantId(variant.getId())
                        .unitPrice(new BigDecimal("19.90"))
                        .quantity(1)
                        .saleDate(SALE_DATE)
                        .build()
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no coincide");

        assertThat(getVariantStock(variant.getId())).isEqualTo(5);
        assertThat(getStock(otherProduct.getId())).isEqualTo(5);
    }

    @Test
    void createManualVariantProductSaleShouldReturnConflictWhenQuantityExceedsVariantStock() {
        ClientEntity client = saveClient("Cliente variante sin stock");
        InventoryProductEntity product = saveVariantProduct("Camiseta Sin Stock");
        InventoryProductVariantEntity variant = saveVariant(product, "Blanca / S", "CAM-BL-S", 2);

        assertThatThrownBy(() -> saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductVariantId(variant.getId())
                        .unitPrice(new BigDecimal("19.90"))
                        .quantity(5)
                        .saleDate(SALE_DATE)
                        .build()
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("No hay stock suficiente para la variante con id: " + variant.getId());

        assertThat(getVariantStock(variant.getId())).isEqualTo(2);
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
                        .notes("Venta de servicio")
                        .build()
        );

        assertThat(sale.getInventoryProductId()).isNull();
        assertThat(sale.getInventoryProductVariantId()).isNull();
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
                        .build()
        );

        assertThat(updatedSale.getInventoryProductId()).isEqualTo(product.getId());
        assertThat(updatedSale.getItemName()).isEqualTo(product.getName());
        assertThat(getStock(product.getId())).isEqualTo(16);
    }

    @Test
    void updateProductSaleShouldRestoreSimpleStockAndDiscountVariantStock() {
        ClientEntity client = saveClient("Cliente update variante");
        InventoryProductEntity simpleProduct = saveProduct("Serum Simple", 10);
        InventoryProductEntity variantProduct = saveVariantProduct("Camiseta Cambio");
        InventoryProductVariantEntity variant = saveVariant(variantProduct, "Azul / M", "CAM-AZ-M", 6);

        SaleResponse createdSale = saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductId(simpleProduct.getId())
                        .unitPrice(new BigDecimal("8.00"))
                        .quantity(2)
                        .saleDate(SALE_DATE)
                        .build()
        );

        SaleResponse updatedSale = saleService.updateSale(
                createdSale.getId(),
                UpdateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductId(variantProduct.getId())
                        .inventoryProductVariantId(variant.getId())
                        .unitPrice(new BigDecimal("18.00"))
                        .quantity(3)
                        .saleDate(SALE_DATE)
                        .build()
        );

        assertThat(updatedSale.getInventoryProductId()).isEqualTo(variantProduct.getId());
        assertThat(updatedSale.getInventoryProductVariantId()).isEqualTo(variant.getId());
        assertThat(updatedSale.getItemName()).isEqualTo("Camiseta Cambio - Azul / M");
        assertThat(getStock(simpleProduct.getId())).isEqualTo(10);
        assertThat(getVariantStock(variant.getId())).isEqualTo(3);
    }

    @Test
    void updateProductSaleShouldRestoreVariantStockAndDiscountSimpleStock() {
        ClientEntity client = saveClient("Cliente update simple");
        InventoryProductEntity variantProduct = saveVariantProduct("Camiseta Original");
        InventoryProductVariantEntity variant = saveVariant(variantProduct, "Negra / L", "CAM-NE-L", 7);
        InventoryProductEntity simpleProduct = saveProduct("Aceite Simple", 9);

        SaleResponse createdSale = saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductVariantId(variant.getId())
                        .unitPrice(new BigDecimal("18.00"))
                        .quantity(3)
                        .saleDate(SALE_DATE)
                        .build()
        );

        SaleResponse updatedSale = saleService.updateSale(
                createdSale.getId(),
                UpdateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductId(simpleProduct.getId())
                        .unitPrice(new BigDecimal("9.00"))
                        .quantity(4)
                        .saleDate(SALE_DATE)
                        .build()
        );

        assertThat(updatedSale.getInventoryProductId()).isEqualTo(simpleProduct.getId());
        assertThat(updatedSale.getInventoryProductVariantId()).isNull();
        assertThat(updatedSale.getItemName()).isEqualTo(simpleProduct.getName());
        assertThat(getVariantStock(variant.getId())).isEqualTo(7);
        assertThat(getStock(simpleProduct.getId())).isEqualTo(5);
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
                        .build()
        );

        saleService.deleteSale(sale.getId());

        assertThat(getStock(product.getId())).isEqualTo(12);
        assertThat(saleRepository.findById(sale.getId())).isEmpty();
    }

    @Test
    void deleteManualVariantProductSaleShouldRestoreVariantStock() {
        ClientEntity client = saveClient("Cliente delete variante");
        InventoryProductEntity product = saveVariantProduct("Camiseta Delete");
        InventoryProductVariantEntity variant = saveVariant(product, "Verde / S", "CAM-VE-S", 9);

        SaleResponse sale = saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductVariantId(variant.getId())
                        .unitPrice(new BigDecimal("17.00"))
                        .quantity(4)
                        .saleDate(SALE_DATE)
                        .build()
        );

        saleService.deleteSale(sale.getId());

        assertThat(getVariantStock(variant.getId())).isEqualTo(9);
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
        appointment.setAppointmentTime(APPOINTMENT_TIME);
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

    private InventoryProductEntity saveVariantProduct(String name) {
        CategoryEntity category = saveCategory();

        return inventoryProductRepository.saveAndFlush(
                InventoryProductEntity.builder()
                        .name(name)
                        .description("Producto con variantes test")
                        .category(category)
                        .stock(0)
                        .minimumStock(0)
                        .sku(null)
                        .purchasePrice(null)
                        .hasVariants(true)
                        .active(true)
                        .build()
        );
    }

    private InventoryProductVariantEntity saveVariant(
            InventoryProductEntity product,
            String variantLabel,
            String sku,
            int stock
    ) {
        return inventoryProductVariantRepository.saveAndFlush(
                InventoryProductVariantEntity.builder()
                        .product(product)
                        .variantLabel(variantLabel)
                        .sku(sku)
                        .stock(stock)
                        .minimumStock(1)
                        .purchasePrice(new BigDecimal("4.00"))
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

    private int getVariantStock(Long variantId) {
        return inventoryProductVariantRepository.findById(variantId)
                .orElseThrow()
                .getStock();
    }

    private int getVariantStockByProduct(Long productId) {
        return inventoryProductVariantRepository.findByProductIdAndActiveTrueOrderByVariantLabelAscIdAsc(productId)
                .stream()
                .mapToInt(InventoryProductVariantEntity::getStock)
                .sum();
    }

    private SaleEntity getSale(Long saleId) {
        return saleRepository.findById(saleId)
                .orElseThrow();
    }
}
