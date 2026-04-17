package es.pedro.pymes_backend.infrastructure.web;

import es.pedro.pymes_backend.domain.CategoryEntity;
import es.pedro.pymes_backend.domain.ClientEntity;
import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.domain.SaleEntity;
import es.pedro.pymes_backend.domain.SaleItemType;
import es.pedro.pymes_backend.domain.SaleSourceType;
import es.pedro.pymes_backend.domain.ServiceEntity;
import es.pedro.pymes_backend.infrastructure.repository.CategoryRepository;
import es.pedro.pymes_backend.infrastructure.repository.ClientRepository;
import es.pedro.pymes_backend.infrastructure.repository.ExpenseRepository;
import es.pedro.pymes_backend.infrastructure.repository.InventoryProductRepository;
import es.pedro.pymes_backend.infrastructure.repository.SaleRepository;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApiIntegrationSmokeTest extends AuthenticatedApiIntegrationTestSupport {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
    private static final Pattern IDENTIFICADOR_PATTERN = Pattern.compile("\"identificador\"\\s*:\\s*(\\d+)");
    private static final LocalDate BASE_DATE = LocalDate.of(2026, 3, 26);
    private static final LocalTime BASE_TIME = LocalTime.of(10, 30);

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private InventoryProductRepository inventoryProductRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = buildAuthenticatedMockMvc();
    }

    @Test
    void categoryClientAndServiceApisShouldValidateSearchAndProtectRelationships() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "color": "blue",
                                  "icon": "tag"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        String categoryName = unique("Category");
        Long categoryId = extractId(
                mockMvc.perform(post("/api/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "%s",
                                          "color": "green",
                                          "icon": "leaf"
                                        }
                                        """.formatted(categoryName)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.name").value(categoryName))
                        .andReturn()
        );

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "color": "green",
                                  "icon": "leaf"
                                }
                                """.formatted(categoryName.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        String clientName = unique("ClientApi");
        Long clientId = extractId(
                mockMvc.perform(post("/api/clients")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fullName": "%s",
                                          "phone": "600111222",
                                          "email": "api.client@test.com"
                                        }
                                        """.formatted(clientName)))
                        .andExpect(status().isCreated())
                        .andReturn()
        );

        mockMvc.perform(get("/api/clients")
                        .param("search", "api.client")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(clientId.intValue()));

        String serviceName = unique("Peinado");
        Long serviceId = extractId(
                mockMvc.perform(post("/api/services")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "%s",
                                          "description": "Servicio api",
                                          "durationMinutes": 45,
                                          "price": 30.0,
                                          "active": true,
                                          "categoryId": %d
                                        }
                                        """.formatted(serviceName, categoryId)))
                        .andExpect(status().isCreated())
                        .andReturn()
        );

        mockMvc.perform(get("/api/services")
                        .param("search", serviceName)
                        .param("active", "true")
                        .param("categoryId", String.valueOf(categoryId))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(serviceId.intValue()))
                .andExpect(jsonPath("$.content[0].category.id").value(categoryId.intValue()));

        mockMvc.perform(delete("/api/categories/{id}", categoryId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void clientApiShouldSupportInvoiceFieldsAndClearThemWhenDisabled() throws Exception {
        ensureClientIdentifierSequence();

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "%s",
                                  "phone": "600111222",
                                  "email": "missing.invoice@test.com",
                                  "admiteFacturas": true
                                }
                                """.formatted(unique("ClientInvoiceInvalid"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        String clientName = unique("ClientInvoice");
        MvcResult createResult = mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "%s",
                                  "phone": "600111222",
                                  "email": "invoice.client@test.com",
                                  "admiteFacturas": true,
                                  "direccion": "Calle Mayor 1",
                                  "cifNif": "B12345678"
                                }
                                """.formatted(clientName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.admiteFacturas").value(true))
                .andExpect(jsonPath("$.direccion").value("Calle Mayor 1"))
                .andExpect(jsonPath("$.cifNif").value("B12345678"))
                .andExpect(jsonPath("$.identificador").isNumber())
                .andReturn();

        Long clientId = extractId(createResult);
        Long identificador = extractIdentificador(createResult);

        mockMvc.perform(put("/api/clients/{id}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "%s",
                                  "phone": "600333444",
                                  "email": "invoice.client.updated@test.com",
                                  "admiteFacturas": true,
                                  "direccion": "Avenida Norte 2",
                                  "cifNif": "B87654321"
                                }
                                """.formatted(clientName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value(identificador.intValue()))
                .andExpect(jsonPath("$.direccion").value("Avenida Norte 2"))
                .andExpect(jsonPath("$.cifNif").value("B87654321"));

        mockMvc.perform(put("/api/clients/{id}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "%s",
                                  "phone": "600333444",
                                  "email": "invoice.client.updated@test.com",
                                  "admiteFacturas": false,
                                  "direccion": "No debe guardarse",
                                  "cifNif": "No debe guardarse"
                                }
                                """.formatted(clientName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admiteFacturas").value(false))
                .andExpect(jsonPath("$.direccion").value(nullValue()))
                .andExpect(jsonPath("$.cifNif").value(nullValue()))
                .andExpect(jsonPath("$.identificador").value(nullValue()));
    }

    @Test
    void inventoryAndExpenseApisShouldSyncStockSupportSuggestionsAndLogicalDelete() throws Exception {
        CategoryEntity category = saveCategory("Inventory");
        String productName = unique("ChampuApi");

        Long productId = extractId(
                mockMvc.perform(post("/api/inventory/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "%s",
                                          "description": "Producto api",
                                          "categoryId": %d,
                                          "stock": 15,
                                          "minimumStock": 5,
                                          "sku": "INV-%s",
                                          "purchasePrice": 18.50
                                        }
                                        """.formatted(productName, category.getId(), UUID.randomUUID())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.name").value(productName))
                        .andReturn()
        );

        mockMvc.perform(get("/api/inventory/products")
                        .param("search", productName)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(productId.intValue()));

        assertThat(getStock(productId)).isEqualTo(15);
        assertThat(expenseRepository.findAll().stream()
                .filter(expense -> productName.equals(expense.getConcept()))
                .toList())
                .hasSize(1);

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expenseDate": "%s",
                                  "concept": "%s",
                                  "category": "SUPPLIES",
                                  "quantity": 5,
                                  "amount": 120.00,
                                  "notes": "Reposicion api"
                                }
                                """.formatted(BASE_DATE, productName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concept").value(productName))
                .andExpect(jsonPath("$.category").value("SUPPLIES"));

        assertThat(getStock(productId)).isEqualTo(20);

        String freeConcept = unique("Proveedor");
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expenseDate": "%s",
                                  "concept": "%s",
                                  "category": "OTHER",
                                  "quantity": 1,
                                  "amount": 35.00,
                                  "notes": "Historico api"
                                }
                                """.formatted(BASE_DATE, freeConcept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concept").value(freeConcept))
                .andExpect(jsonPath("$.category").value("OTHER"));

        mockMvc.perform(get("/api/expenses/suggestions")
                        .param("query", "champu")
                        .param("category", "SUPPLIES")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(productName));

        mockMvc.perform(get("/api/expenses/suggestions")
                        .param("query", "prove")
                        .param("category", "OTHER")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(freeConcept));

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expenseDate": "%s",
                                  "concept": "Producto inexistente api",
                                  "category": "SUPPLIES",
                                  "quantity": 2,
                                  "amount": 10.00,
                                  "notes": "Debe fallar"
                                }
                                """.formatted(BASE_DATE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/api/inventory/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "Producto invalido",
                                  "categoryId": %d,
                                  "stock": 3,
                                  "minimumStock": 1,
                                  "sku": "INV-%s",
                                  "purchasePrice": null
                                }
                                """.formatted(unique("SinPrecio"), category.getId(), UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/inventory/products/export/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));

        mockMvc.perform(delete("/api/inventory/products/{id}", productId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/inventory/products")
                        .param("search", productName)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void expensePromptManualCasesShouldSupportSuppliesSuggestionsExactMatchValidationAndFreeServicesText() throws Exception {
        InventoryProductEntity firstProduct = saveProduct("Champ\u00fa Klorane", 8);
        InventoryProductEntity secondProduct = saveProduct("Champ\u00fa L'Oreal Prof", 15);

        mockMvc.perform(get("/api/expenses/suggestions")
                        .param("query", "cha")
                        .param("category", "SUPPLIES")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Champ\u00fa Klorane"))
                .andExpect(jsonPath("$[1]").value("Champ\u00fa L'Oreal Prof"));

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expenseDate": "2026-03-26",
                                  "concept": "Champ\u00fa L'Oreal Prof",
                                  "category": "SUPPLIES",
                                  "quantity": 5,
                                  "amount": 120.00,
                                  "notes": "Reposici\u00f3n"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concept").value("Champ\u00fa L'Oreal Prof"))
                .andExpect(jsonPath("$.category").value("SUPPLIES"));

        assertThat(getStock(secondProduct.getId())).isEqualTo(20);

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expenseDate": "2026-03-26",
                                  "concept": "Compra champ\u00fa loreal",
                                  "category": "SUPPLIES",
                                  "quantity": 5,
                                  "amount": 120.00,
                                  "notes": "Reposici\u00f3n"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertThat(getStock(secondProduct.getId())).isEqualTo(20);

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expenseDate": "2026-03-26",
                                  "concept": "Factura luz marzo",
                                  "category": "SERVICES",
                                  "quantity": 1,
                                  "amount": 450.00,
                                  "notes": "Pago mensual"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concept").value("Factura luz marzo"))
                .andExpect(jsonPath("$.category").value("SERVICES"));

        assertThat(getStock(firstProduct.getId())).isEqualTo(8);
        assertThat(getStock(secondProduct.getId())).isEqualTo(20);
    }

    @Test
    void salesApisShouldDiscountRestoreStockAndReportConflicts() throws Exception {
        ClientEntity client = saveClient("ClientSales");
        CategoryEntity serviceCategory = saveCategory("ServiceSales");
        ServiceEntity service = saveService("Servicio Sales Api", 42.0, serviceCategory);
        InventoryProductEntity product = saveProduct("ChampuSales", 20);
        int recentDaysWindow = Math.max(1, (int) ChronoUnit.DAYS.between(BASE_DATE, LocalDate.now()) + 1);

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "itemType": "SERVICE",
                                  "serviceId": %d,
                                  "quantity": 1,
                                  "saleDate": "%s",
                                  "saleTime": "%s",
                                  "notes": "Venta servicio api"
                                }
                                """.formatted(client.getId(), service.getId(), BASE_DATE, BASE_TIME.minusHours(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceId").value(service.getId().intValue()))
                .andExpect(jsonPath("$.inventoryProductId").value(nullValue()))
                .andExpect(jsonPath("$.itemName").value(service.getName()))
                .andExpect(jsonPath("$.unitPrice").value(42.0))
                .andExpect(jsonPath("$.totalAmount").value(42.0));

        assertThat(getStock(product.getId())).isEqualTo(20);

        Long saleId = extractId(
                mockMvc.perform(post("/api/sales")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "clientId": %d,
                                          "itemType": "PRODUCT",
                                          "inventoryProductId": %d,
                                          "itemName": "Ignorado",
                                          "unitPrice": 12.50,
                                          "quantity": 2,
                                          "saleDate": "%s",
                                          "saleTime": "%s",
                                          "notes": "Venta api"
                                        }
                                        """.formatted(client.getId(), product.getId(), BASE_DATE, BASE_TIME)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.inventoryProductId").value(product.getId().intValue()))
                        .andExpect(jsonPath("$.itemName").value(product.getName()))
                        .andReturn()
        );

        assertThat(getStock(product.getId())).isEqualTo(18);

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "itemType": "PRODUCT",
                                  "inventoryProductId": %d,
                                  "unitPrice": 12.50,
                                  "quantity": 50,
                                  "saleDate": "%s",
                                  "saleTime": "%s"
                                }
                                """.formatted(client.getId(), product.getId(), BASE_DATE, BASE_TIME.plusHours(1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(getStock(product.getId())).isEqualTo(18);

        mockMvc.perform(put("/api/sales/{id}", saleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "itemType": "PRODUCT",
                                  "inventoryProductId": %d,
                                  "unitPrice": 12.50,
                                  "quantity": 4,
                                  "saleDate": "%s",
                                  "saleTime": "%s"
                                }
                                """.formatted(client.getId(), product.getId(), BASE_DATE, BASE_TIME.plusHours(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(4));

        assertThat(getStock(product.getId())).isEqualTo(16);

        mockMvc.perform(get("/api/sales/recent")
                        .param("days", String.valueOf(recentDaysWindow))
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(saleId.intValue()));

        mockMvc.perform(delete("/api/sales/{id}", saleId))
                .andExpect(status().isNoContent());

        assertThat(getStock(product.getId())).isEqualTo(20);
    }

    @Test
    void appointmentConfirmationApiShouldCreateAutomaticServiceSaleWithoutInventoryImpact() throws Exception {
        ClientEntity client = saveClient("ClientAppointment");
        CategoryEntity category = saveCategory("Services");
        ServiceEntity service = saveService("FacialApi", 35.0, category);
        InventoryProductEntity product = saveProduct("FoamApi", 9);

        Long appointmentId = extractId(
                mockMvc.perform(post("/api/appointments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "clientId": %d,
                                          "serviceId": %d,
                                          "price": 35.0,
                                          "professionalName": "Laura",
                                          "appointmentDate": "%s",
                                          "appointmentTime": "%s",
                                          "status": "PENDING",
                                          "internalNotes": "Cita api"
                                        }
                                        """.formatted(client.getId(), service.getId(), BASE_DATE, BASE_TIME)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("PENDING"))
                        .andReturn()
        );

        mockMvc.perform(put("/api/appointments/{id}", appointmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "serviceId": %d,
                                  "price": 35.0,
                                  "professionalName": "Laura",
                                  "appointmentDate": "%s",
                                  "appointmentTime": "%s",
                                  "status": "CONFIRMED",
                                  "internalNotes": "Cita confirmada"
                                }
                                """.formatted(client.getId(), service.getId(), BASE_DATE, BASE_TIME.plusHours(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        SaleEntity appointmentSale = saleRepository.findByAppointmentId(appointmentId)
                .orElseThrow();

        assertThat(appointmentSale.getSourceType()).isEqualTo(SaleSourceType.APPOINTMENT);
        assertThat(appointmentSale.getItemType()).isEqualTo(SaleItemType.SERVICE);
        assertThat(appointmentSale.getInventoryProduct()).isNull();
        assertThat(getStock(product.getId())).isEqualTo(9);

        mockMvc.perform(put("/api/appointments/{id}", appointmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "serviceId": %d,
                                  "price": 35.0,
                                  "professionalName": "Laura",
                                  "appointmentDate": "%s",
                                  "appointmentTime": "%s",
                                  "status": "CONFIRMED",
                                  "internalNotes": "Cita reconfirmada"
                                }
                                """.formatted(client.getId(), service.getId(), BASE_DATE, BASE_TIME.plusHours(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertThat(saleRepository.findAll().stream()
                .filter(sale -> appointmentId.equals(sale.getAppointmentId()))
                .toList())
                .hasSize(1);
        assertThat(saleRepository.findByAppointmentId(appointmentId).orElseThrow().getId())
                .isEqualTo(appointmentSale.getId());

        mockMvc.perform(delete("/api/sales/{id}", appointmentSale.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(getStock(product.getId())).isEqualTo(9);
    }

    @Test
    void apiShouldReturnConsistentNotFoundAndBadRequestResponsesForCommonFailures() throws Exception {
        mockMvc.perform(get("/api/clients/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(get("/api/expenses")
                        .param("fromDate", "2026-03-27")
                        .param("toDate", "2026-03-26")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private Long extractId(MvcResult result) throws Exception {
        Matcher matcher = ID_PATTERN.matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new IllegalStateException("No se pudo extraer el id de la respuesta: " + result.getResponse().getContentAsString());
        }
        return Long.parseLong(matcher.group(1));
    }

    private Long extractIdentificador(MvcResult result) throws Exception {
        Matcher matcher = IDENTIFICADOR_PATTERN.matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new IllegalStateException("No se pudo extraer el identificador de la respuesta: " + result.getResponse().getContentAsString());
        }
        return Long.parseLong(matcher.group(1));
    }

    private void ensureClientIdentifierSequence() {
        entityManager.createNativeQuery("CREATE SEQUENCE IF NOT EXISTS clients_identificador_seq START WITH 1 INCREMENT BY 1")
                .executeUpdate();
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private CategoryEntity saveCategory(String prefix) {
        return categoryRepository.saveAndFlush(
                CategoryEntity.builder()
                        .name(unique(prefix))
                        .color("blue")
                        .icon("tag")
                        .build()
        );
    }

    private ClientEntity saveClient(String prefix) {
        ClientEntity client = new ClientEntity();
        client.setFullName(unique(prefix));
        client.setPhone("600555444");
        client.setEmail(UUID.randomUUID() + "@test.com");
        return clientRepository.saveAndFlush(client);
    }

    private InventoryProductEntity saveProduct(String name, int stock) {
        CategoryEntity category = saveCategory("InventoryCategory");

        return inventoryProductRepository.saveAndFlush(
                InventoryProductEntity.builder()
                        .name(name)
                        .description("Producto api")
                        .category(category)
                        .stock(stock)
                        .minimumStock(3)
                        .sku("SKU-" + UUID.randomUUID())
                        .purchasePrice(new BigDecimal("10.00"))
                        .active(true)
                        .build()
        );
    }

    private ServiceEntity saveService(String name, double price, CategoryEntity category) {
        return serviceRepository.saveAndFlush(
                ServiceEntity.builder()
                        .name(name)
                        .description("Servicio api")
                        .durationMinutes(60)
                        .price(price)
                        .active(true)
                        .category(category)
                        .build()
        );
    }

    private int getStock(Long productId) {
        return inventoryProductRepository.findById(productId)
                .orElseThrow()
                .getStock();
    }
}
