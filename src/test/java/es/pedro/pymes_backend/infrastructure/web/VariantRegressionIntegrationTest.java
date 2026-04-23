package es.pedro.pymes_backend.infrastructure.web;

import es.pedro.pymes_backend.application.AppointmentService;
import es.pedro.pymes_backend.application.ExpenseService;
import es.pedro.pymes_backend.application.InventoryProductService;
import es.pedro.pymes_backend.application.SaleService;
import es.pedro.pymes_backend.domain.AppointmentStatus;
import es.pedro.pymes_backend.domain.CategoryEntity;
import es.pedro.pymes_backend.domain.ClientEntity;
import es.pedro.pymes_backend.domain.ExpenseCategory;
import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.domain.InventoryProductVariantEntity;
import es.pedro.pymes_backend.domain.SaleEntity;
import es.pedro.pymes_backend.domain.SaleItemType;
import es.pedro.pymes_backend.domain.SaleSourceType;
import es.pedro.pymes_backend.domain.ServiceEntity;
import es.pedro.pymes_backend.infrastructure.repository.CategoryRepository;
import es.pedro.pymes_backend.infrastructure.repository.ClientRepository;
import es.pedro.pymes_backend.infrastructure.repository.InventoryProductRepository;
import es.pedro.pymes_backend.infrastructure.repository.InventoryProductVariantRepository;
import es.pedro.pymes_backend.infrastructure.repository.SaleRepository;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.AppointmentRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.AppointmentResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateExpenseRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateInventoryProductRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateInventoryProductVariantRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateSaleRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.ExpenseResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.InventoryProductResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.SaleResponse;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
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
class VariantRegressionIntegrationTest extends AuthenticatedApiIntegrationTestSupport {

    private static final YearMonth REPORT_MONTH = YearMonth.of(2026, 6);

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private InventoryProductService inventoryProductService;

    @Autowired
    private SaleService saleService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private InventoryProductRepository inventoryProductRepository;

    @Autowired
    private InventoryProductVariantRepository inventoryProductVariantRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = buildAuthenticatedMockMvc();
    }

    @Test
    void financialReportsShouldIncludeSimpleAndVariantSalesAndExpenses() throws Exception {
        RegressionData data = seedFinancialVariantData(REPORT_MONTH);

        mockMvc.perform(get("/api/reports/monthly/simple")
                        .param("year", String.valueOf(REPORT_MONTH.getYear()))
                        .param("month", String.valueOf(REPORT_MONTH.getMonthValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(65.0))
                .andExpect(jsonPath("$.totalExpenses").value(20.0));

        mockMvc.perform(get("/api/reports/monthly/detailed")
                        .param("year", String.valueOf(REPORT_MONTH.getYear()))
                        .param("month", String.valueOf(REPORT_MONTH.getMonthValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossIncome").value(65.0))
                .andExpect(jsonPath("$.grossExpenses").value(20.0))
                .andExpect(jsonPath("$.operatingProfit").value(45.0))
                .andExpect(jsonPath("$.estimatedVatPayable").exists())
                .andExpect(jsonPath("$.estimatedIrpfPayable").exists())
                .andExpect(jsonPath("$.estimatedTotalTaxes").exists())
                .andExpect(jsonPath("$.profitabilityRatio").exists());

        assertThat(getStock(data.simpleProduct().getId())).isEqualTo(52);
        assertThat(getVariantStock(data.variantProduct().firstVariant().getId())).isEqualTo(19);
    }

    @Test
    void productRankingShouldGroupVariantSalesByBaseProduct() throws Exception {
        CategoryEntity category = saveCategory("Ranking products");
        ClientEntity client = saveClient("Ranking Client");
        VariantProduct variantProduct = saveVariantProduct(
                "Ranking Camiseta",
                category,
                "Roja / M",
                20,
                "Azul / L",
                20
        );

        createVariantProductSale(
                client,
                variantProduct.firstVariant(),
                2,
                new BigDecimal("10.00"),
                LocalDate.of(2026, 7, 10),
                LocalTime.of(10, 0)
        );
        createVariantProductSale(
                client,
                variantProduct.secondVariant(),
                3,
                new BigDecimal("12.00"),
                LocalDate.of(2026, 7, 11),
                LocalTime.of(11, 0)
        );

        mockMvc.perform(get("/api/reports/ranking")
                        .param("type", "PRODUCT")
                        .param("year", "2026")
                        .param("month", "7")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PRODUCT"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value(variantProduct.product().getName()))
                .andExpect(jsonPath("$.content[0].totalCount").value(5))
                .andExpect(jsonPath("$.content[0].generatedIncome").value(56.0));
    }

    @Test
    void pdfExportsShouldReturnPdfContentWithVariantData() throws Exception {
        seedFinancialVariantData(YearMonth.of(2026, 8));

        assertPdfEndpoint("/api/sales/export/pdf?fromDate=2026-08-01&toDate=2026-08-31");
        assertPdfEndpoint("/api/expenses/export/pdf?fromDate=2026-08-01&toDate=2026-08-31");
        assertPdfEndpoint("/api/inventory/products/export/pdf");
        assertPdfEndpoint("/api/reports/monthly/detailed/pdf?year=2026&month=8");
        assertPdfEndpoint("/api/reports/quarterly/detailed/pdf?year=2026&quarter=3");
        assertPdfEndpoint("/api/reports/annual/consolidated/pdf?year=2026");
    }

    @Test
    void inventoryCompatibilityShouldPreserveSimpleAndVariantRules() throws Exception {
        CategoryEntity category = saveCategory("Inventory compatibility");

        InventoryProductResponse simpleProduct = inventoryProductService.createProduct(
                CreateInventoryProductRequest.builder()
                        .name(uniqueName("Simple Inventory Product"))
                        .description("Simple stock regression")
                        .categoryId(category.getId())
                        .stock(7)
                        .minimumStock(2)
                        .sku(uniqueSku("SIMPLE"))
                        .purchasePrice(new BigDecimal("4.00"))
                        .hasVariants(false)
                        .build()
        );
        assertThat(simpleProduct.getHasVariants()).isFalse();
        assertThat(simpleProduct.getStock()).isEqualTo(7);

        InventoryProductResponse variantProduct = inventoryProductService.createProduct(
                CreateInventoryProductRequest.builder()
                        .name(uniqueName("Variant Inventory Product"))
                        .description("Variant stock regression")
                        .categoryId(category.getId())
                        .hasVariants(true)
                        .variants(List.of(
                                CreateInventoryProductVariantRequest.builder()
                                        .variantLabel("Roja / M")
                                        .sku(uniqueSku("VAR-A"))
                                        .stock(3)
                                        .minimumStock(1)
                                        .purchasePrice(new BigDecimal("5.00"))
                                        .build(),
                                CreateInventoryProductVariantRequest.builder()
                                        .variantLabel("Azul / L")
                                        .sku(uniqueSku("VAR-B"))
                                        .stock(4)
                                        .minimumStock(1)
                                        .purchasePrice(new BigDecimal("6.00"))
                                        .build()
                        ))
                        .build()
        );

        InventoryProductResponse variantDetail = inventoryProductService.getProductById(variantProduct.getId());
        assertThat(variantDetail.getHasVariants()).isTrue();
        assertThat(variantDetail.getVariantCount()).isEqualTo(2);
        assertThat(variantDetail.getStock()).isEqualTo(7);
        assertThat(variantDetail.getVariants()).hasSize(2);

        inventoryProductService.deleteProduct(variantProduct.getId());
        assertThat(inventoryProductVariantRepository.findByProductIdOrderByVariantLabelAscIdAsc(variantProduct.getId()))
                .allSatisfy(variant -> assertThat(variant.getActive()).isFalse());

        mockMvc.perform(put("/api/inventory/products/{id}", simpleProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(convertToVariantsJson(simpleProduct.getName(), category.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inventoryProductApiShouldExposeBaseProductsAndActiveVariantDetailForFrontendSelectors() throws Exception {
        CategoryEntity category = saveCategory("Frontend inventory");
        InventoryProductEntity simpleProduct = saveSimpleProduct("Frontend Simple Product", category, 9);
        VariantProduct variantProduct = saveVariantProduct(
                "Frontend Variant Product",
                category,
                "Roja / M",
                4,
                "Azul / L",
                6
        );

        variantProduct.secondVariant().setActive(false);
        inventoryProductVariantRepository.saveAndFlush(variantProduct.secondVariant());

        mockMvc.perform(get("/api/inventory/products")
                        .param("search", variantProduct.product().getName())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(variantProduct.product().getId().intValue()))
                .andExpect(jsonPath("$.content[0].hasVariants").value(true))
                .andExpect(jsonPath("$.content[0].variantCount").value(1))
                .andExpect(jsonPath("$.content[0].stock").value(4))
                .andExpect(jsonPath("$.content[0].variants", hasSize(0)));

        mockMvc.perform(get("/api/inventory/products/{id}", variantProduct.product().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(variantProduct.product().getId().intValue()))
                .andExpect(jsonPath("$.hasVariants").value(true))
                .andExpect(jsonPath("$.variantCount").value(1))
                .andExpect(jsonPath("$.stock").value(4))
                .andExpect(jsonPath("$.variants", hasSize(1)))
                .andExpect(jsonPath("$.variants[0].id").value(variantProduct.firstVariant().getId().intValue()))
                .andExpect(jsonPath("$.variants[0].variantLabel").value("Roja / M"))
                .andExpect(jsonPath("$.variants[0].stock").value(4))
                .andExpect(jsonPath("$.variants[0].active").value(true));

        mockMvc.perform(get("/api/inventory/products")
                        .param("search", simpleProduct.getName())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(simpleProduct.getId().intValue()))
                .andExpect(jsonPath("$.content[0].hasVariants").value(false))
                .andExpect(jsonPath("$.content[0].stock").value(9))
                .andExpect(jsonPath("$.content[0].variants", hasSize(0)));
    }

    @Test
    void expensesCompatibilityShouldHandleSimpleVariantAndRejectNonSuppliesInventoryReferences() throws Exception {
        CategoryEntity category = saveCategory("Expenses compatibility");
        InventoryProductEntity simpleProduct = saveSimpleProduct("Expense Simple Product", category, 10);
        VariantProduct variantProduct = saveVariantProduct(
                "Expense Variant Product",
                category,
                "Roja / M",
                5,
                "Azul / L",
                6
        );

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("Proveedor simple", ExpenseCategory.SUPPLIES, 3,
                                new BigDecimal("9.00"), simpleProduct.getId(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concept").value(simpleProduct.getName()))
                .andExpect(jsonPath("$.inventoryProductId").value(simpleProduct.getId()))
                .andExpect(jsonPath("$.inventoryProductVariantId").value(nullValue()));

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("Proveedor variante", ExpenseCategory.SUPPLIES, 2,
                                new BigDecimal("8.00"), null, variantProduct.firstVariant().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concept").value(variantProduct.product().getName() + " - Roja / M"))
                .andExpect(jsonPath("$.inventoryProductId").value(variantProduct.product().getId()))
                .andExpect(jsonPath("$.inventoryProductVariantId").value(variantProduct.firstVariant().getId()));

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("Servicio con inventario", ExpenseCategory.SERVICES, 1,
                                new BigDecimal("15.00"), simpleProduct.getId(), null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("Otro con variante", ExpenseCategory.OTHER, 1,
                                new BigDecimal("15.00"), null, variantProduct.firstVariant().getId())))
                .andExpect(status().isBadRequest());

        assertThat(getStock(simpleProduct.getId())).isEqualTo(13);
        assertThat(getVariantStock(variantProduct.firstVariant().getId())).isEqualTo(7);
    }

    @Test
    void salesCompatibilityShouldPreserveServicesAppointmentsSimpleAndVariantStock() throws Exception {
        CategoryEntity category = saveCategory("Sales compatibility");
        ClientEntity client = saveClient("Sales Client");
        ServiceEntity service = saveService("Servicio Basico", category);
        InventoryProductEntity simpleProduct = saveSimpleProduct("Sale Simple Product", category, 10);
        VariantProduct variantProduct = saveVariantProduct(
                "Sale Variant Product",
                category,
                "Roja / M",
                10,
                "Azul / L",
                10
        );

        SaleResponse serviceSale = createServiceSale(client, service, LocalDate.of(2026, 9, 1), LocalTime.of(10, 0));
        assertThat(serviceSale.getItemType()).isEqualTo(SaleItemType.SERVICE);
        assertThat(serviceSale.getInventoryProductId()).isNull();
        assertThat(serviceSale.getInventoryProductVariantId()).isNull();
        assertThat(getStock(simpleProduct.getId())).isEqualTo(10);
        assertThat(getVariantStock(variantProduct.firstVariant().getId())).isEqualTo(10);

        AppointmentResponse pendingAppointment = appointmentService.createAppointment(
                buildAppointmentRequest(client, service, AppointmentStatus.PENDING)
        );
        appointmentService.updateAppointment(
                pendingAppointment.getId(),
                buildAppointmentRequest(client, service, AppointmentStatus.CONFIRMED)
        );

        SaleEntity appointmentSale = saleRepository.findByAppointmentId(pendingAppointment.getId()).orElseThrow();
        assertThat(appointmentSale.getSourceType()).isEqualTo(SaleSourceType.APPOINTMENT);
        assertThat(appointmentSale.getItemType()).isEqualTo(SaleItemType.SERVICE);
        assertThat(appointmentSale.getInventoryProduct()).isNull();
        assertThat(appointmentSale.getInventoryProductVariant()).isNull();

        mockMvc.perform(delete("/api/sales/{id}", appointmentSale.getId()))
                .andExpect(status().isConflict());

        createSimpleProductSale(client, simpleProduct, 2, new BigDecimal("10.00"),
                LocalDate.of(2026, 9, 3), LocalTime.of(11, 0));
        createVariantProductSale(client, variantProduct.firstVariant(), 3, new BigDecimal("12.00"),
                LocalDate.of(2026, 9, 4), LocalTime.of(12, 0));

        assertThat(getStock(simpleProduct.getId())).isEqualTo(8);
        assertThat(getStock(variantProduct.product().getId())).isZero();
        assertThat(getVariantStock(variantProduct.firstVariant().getId())).isEqualTo(7);
    }

    @Test
    void historySearchShouldFindVariantSaleAndExpenseByHistoricalNames() throws Exception {
        CategoryEntity category = saveCategory("History compatibility");
        ClientEntity client = saveClient("History Client");
        VariantProduct variantProduct = saveVariantProduct(
                "History Camiseta",
                category,
                "Roja / M",
                10,
                "Azul / L",
                10
        );

        SaleResponse sale = createVariantProductSale(client, variantProduct.firstVariant(), 2,
                new BigDecimal("14.00"), LocalDate.of(2026, 10, 10), LocalTime.of(10, 0));
        ExpenseResponse expense = expenseService.createExpense(
                buildExpenseRequest("Proveedor historico", ExpenseCategory.SUPPLIES, 1,
                        new BigDecimal("5.00"), null, variantProduct.firstVariant().getId(), LocalDate.of(2026, 10, 10))
        );

        mockMvc.perform(get("/api/sales")
                        .param("search", "Roja")
                        .param("fromDate", "2026-10-01")
                        .param("toDate", "2026-10-31")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(sale.getId()))
                .andExpect(jsonPath("$.content[0].itemName").value(variantProduct.product().getName() + " - Roja / M"))
                .andExpect(jsonPath("$.content[0].inventoryProductVariantId").value(variantProduct.firstVariant().getId()));

        mockMvc.perform(get("/api/expenses")
                        .param("search", "Roja")
                        .param("fromDate", "2026-10-01")
                        .param("toDate", "2026-10-31")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(expense.getId()))
                .andExpect(jsonPath("$.content[0].concept").value(variantProduct.product().getName() + " - Roja / M"))
                .andExpect(jsonPath("$.content[0].inventoryProductVariantId").value(variantProduct.firstVariant().getId()));
    }

    private RegressionData seedFinancialVariantData(YearMonth month) {
        CategoryEntity category = saveCategory("Variant regression");
        ClientEntity client = saveClient("Regression Client");
        InventoryProductEntity simpleProduct = saveSimpleProduct("Regression Simple Product", category, 50);
        VariantProduct variantProduct = saveVariantProduct(
                "Regression Variant Product",
                category,
                "Roja / M",
                20,
                "Azul / L",
                20
        );

        createSimpleProductSale(client, simpleProduct, 2, new BigDecimal("10.00"), month.atDay(10), LocalTime.of(10, 0));
        createVariantProductSale(client, variantProduct.firstVariant(), 3, new BigDecimal("15.00"),
                month.atDay(11), LocalTime.of(11, 0));
        expenseService.createExpense(buildExpenseRequest("Proveedor simple", ExpenseCategory.SUPPLIES, 4,
                new BigDecimal("12.00"), simpleProduct.getId(), null, month.atDay(12)));
        expenseService.createExpense(buildExpenseRequest("Proveedor variante", ExpenseCategory.SUPPLIES, 2,
                new BigDecimal("8.00"), null, variantProduct.firstVariant().getId(), month.atDay(13)));

        return new RegressionData(simpleProduct, variantProduct);
    }

    private SaleResponse createServiceSale(ClientEntity client, ServiceEntity service, LocalDate saleDate, LocalTime saleTime) {
        return saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.SERVICE)
                        .serviceId(service.getId())
                        .quantity(1)
                        .saleDate(saleDate)
                        .saleTime(saleTime)
                        .build()
        );
    }

    private SaleResponse createSimpleProductSale(
            ClientEntity client,
            InventoryProductEntity product,
            int quantity,
            BigDecimal unitPrice,
            LocalDate saleDate,
            LocalTime saleTime
    ) {
        return saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductId(product.getId())
                        .quantity(quantity)
                        .unitPrice(unitPrice)
                        .saleDate(saleDate)
                        .saleTime(saleTime)
                        .build()
        );
    }

    private SaleResponse createVariantProductSale(
            ClientEntity client,
            InventoryProductVariantEntity variant,
            int quantity,
            BigDecimal unitPrice,
            LocalDate saleDate,
            LocalTime saleTime
    ) {
        return saleService.createManualSale(
                CreateSaleRequest.builder()
                        .clientId(client.getId())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProductVariantId(variant.getId())
                        .quantity(quantity)
                        .unitPrice(unitPrice)
                        .saleDate(saleDate)
                        .saleTime(saleTime)
                        .build()
        );
    }

    private CreateExpenseRequest buildExpenseRequest(
            String concept,
            ExpenseCategory category,
            int quantity,
            BigDecimal amount,
            Long inventoryProductId,
            Long inventoryProductVariantId
    ) {
        return buildExpenseRequest(
                concept,
                category,
                quantity,
                amount,
                inventoryProductId,
                inventoryProductVariantId,
                REPORT_MONTH.atDay(12)
        );
    }

    private CreateExpenseRequest buildExpenseRequest(
            String concept,
            ExpenseCategory category,
            int quantity,
            BigDecimal amount,
            Long inventoryProductId,
            Long inventoryProductVariantId,
            LocalDate expenseDate
    ) {
        return CreateExpenseRequest.builder()
                .expenseDate(expenseDate)
                .concept(concept)
                .category(category)
                .quantity(quantity)
                .amount(amount)
                .inventoryProductId(inventoryProductId)
                .inventoryProductVariantId(inventoryProductVariantId)
                .build();
    }

    private String expenseJson(
            String concept,
            ExpenseCategory category,
            int quantity,
            BigDecimal amount,
            Long inventoryProductId,
            Long inventoryProductVariantId
    ) {
        return """
                {
                  "expenseDate": "%s",
                  "concept": "%s",
                  "category": "%s",
                  "quantity": %d,
                  "amount": %s,
                  "inventoryProductId": %s,
                  "inventoryProductVariantId": %s
                }
                """.formatted(
                REPORT_MONTH.atDay(12),
                concept,
                category.name(),
                quantity,
                amount,
                nullableJsonNumber(inventoryProductId),
                nullableJsonNumber(inventoryProductVariantId)
        );
    }

    private String convertToVariantsJson(String productName, Long categoryId) {
        return """
                {
                  "name": "%s",
                  "description": "Attempted conversion",
                  "categoryId": %d,
                  "hasVariants": true,
                  "variants": [
                    {
                      "variantLabel": "Nueva / Variante",
                      "sku": "%s",
                      "stock": 0,
                      "minimumStock": 0
                    }
                  ]
                }
                """.formatted(productName, categoryId, uniqueSku("CONVERT"));
    }

    private String nullableJsonNumber(Long value) {
        return value == null ? "null" : value.toString();
    }

    private AppointmentRequest buildAppointmentRequest(ClientEntity client, ServiceEntity service, AppointmentStatus status) {
        AppointmentRequest request = new AppointmentRequest();
        request.setClientId(client.getId());
        request.setServiceId(service.getId());
        request.setPrice(service.getPrice());
        request.setProfessionalName("Laura");
        request.setAppointmentDate(LocalDate.of(2026, 9, 2));
        request.setAppointmentTime(LocalTime.of(10, 30));
        request.setStatus(status);
        request.setInternalNotes("Appointment regression");
        return request;
    }

    private CategoryEntity saveCategory(String namePrefix) {
        return categoryRepository.saveAndFlush(
                CategoryEntity.builder()
                        .name(uniqueName(namePrefix))
                        .color("blue")
                        .icon("tag")
                        .build()
        );
    }

    private ClientEntity saveClient(String fullNamePrefix) {
        ClientEntity client = new ClientEntity();
        client.setFullName(uniqueName(fullNamePrefix));
        client.setPhone("600000000");
        client.setEmail(uniqueName("client").toLowerCase().replace(" ", ".") + "@test.com");
        return clientRepository.saveAndFlush(client);
    }

    private ServiceEntity saveService(String namePrefix, CategoryEntity category) {
        return serviceRepository.saveAndFlush(
                ServiceEntity.builder()
                        .name(uniqueName(namePrefix))
                        .description("Regression service")
                        .durationMinutes(45)
                        .price(30.0)
                        .active(true)
                        .category(category)
                        .build()
        );
    }

    private InventoryProductEntity saveSimpleProduct(String namePrefix, CategoryEntity category, int stock) {
        return inventoryProductRepository.saveAndFlush(
                InventoryProductEntity.builder()
                        .name(uniqueName(namePrefix))
                        .description("Regression simple product")
                        .category(category)
                        .stock(stock)
                        .minimumStock(2)
                        .sku(uniqueSku("SIMPLE"))
                        .purchasePrice(new BigDecimal("4.00"))
                        .hasVariants(false)
                        .active(true)
                        .build()
        );
    }

    private VariantProduct saveVariantProduct(
            String namePrefix,
            CategoryEntity category,
            String firstLabel,
            int firstStock,
            String secondLabel,
            int secondStock
    ) {
        InventoryProductEntity product = inventoryProductRepository.saveAndFlush(
                InventoryProductEntity.builder()
                        .name(uniqueName(namePrefix))
                        .description("Regression variant product")
                        .category(category)
                        .stock(0)
                        .minimumStock(0)
                        .sku(null)
                        .purchasePrice(null)
                        .hasVariants(true)
                        .active(true)
                        .build()
        );

        InventoryProductVariantEntity firstVariant = saveVariant(product, firstLabel, firstStock);
        InventoryProductVariantEntity secondVariant = saveVariant(product, secondLabel, secondStock);

        return new VariantProduct(product, firstVariant, secondVariant);
    }

    private InventoryProductVariantEntity saveVariant(InventoryProductEntity product, String label, int stock) {
        return inventoryProductVariantRepository.saveAndFlush(
                InventoryProductVariantEntity.builder()
                        .product(product)
                        .variantLabel(label)
                        .sku(uniqueSku("VAR"))
                        .stock(stock)
                        .minimumStock(2)
                        .purchasePrice(new BigDecimal("5.00"))
                        .active(true)
                        .build()
        );
    }

    private void assertPdfEndpoint(String uri) throws Exception {
        MvcResult result = mockMvc.perform(get(uri))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
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

    private String uniqueName(String prefix) {
        return prefix + " " + UUID.randomUUID();
    }

    private String uniqueSku(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private record RegressionData(InventoryProductEntity simpleProduct, VariantProduct variantProduct) {
    }

    private record VariantProduct(
            InventoryProductEntity product,
            InventoryProductVariantEntity firstVariant,
            InventoryProductVariantEntity secondVariant
    ) {
    }
}
