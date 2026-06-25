package es.supplesoftware.autonomos_api.common;

import es.supplesoftware.autonomos_api.categories.infrastructure.CategoryEntity;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductEntity;
import es.supplesoftware.autonomos_api.sales.infrastructure.SaleEntity;
import es.supplesoftware.autonomos_api.categories.infrastructure.CategoryRepository;
import es.supplesoftware.autonomos_api.expenses.infrastructure.ExpenseRepository;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductRepository;
import es.supplesoftware.autonomos_api.sales.infrastructure.SaleRepository;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrnitorrincoCasiDefinitivoApiIntegrationTest extends AuthenticatedApiIntegrationTestSupport {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
    private static final LocalDate BASE_DATE = LocalDate.of(2026, 4, 8);
    private static final LocalTime BASE_TIME = LocalTime.of(10, 30);

    private static final String SETTINGS_JSON = """
            {
              "businessName": "Ornitorrinco Spa",
              "phone": "+34 911 111 111",
              "address": "Calle del Vapor 42, Madrid",
              "contactEmail": "hola@ornitorrinco.test",
              "onlineBookingEnabled": true,
              "defaultAppointmentDurationMinutes": 45,
              "minimumAdvanceMinutes": 30,
              "maximumAdvanceDays": 60,
              "freeCancellationHours": 12,
              "cancellationPolicy": "Cancelacion flexible para pruebas.",
              "openingHours": [
                { "dayOfWeek": "MONDAY", "enabled": true, "openingTime": "09:00", "closingTime": "18:00" },
                { "dayOfWeek": "TUESDAY", "enabled": true, "openingTime": "09:00", "closingTime": "18:00" },
                { "dayOfWeek": "WEDNESDAY", "enabled": true, "openingTime": "09:00", "closingTime": "18:00" },
                { "dayOfWeek": "THURSDAY", "enabled": true, "openingTime": "09:00", "closingTime": "18:00" },
                { "dayOfWeek": "FRIDAY", "enabled": true, "openingTime": "09:00", "closingTime": "19:00" },
                { "dayOfWeek": "SATURDAY", "enabled": true, "openingTime": "10:00", "closingTime": "14:00" },
                { "dayOfWeek": "SUNDAY", "enabled": false, "openingTime": null, "closingTime": null }
              ],
              "specialClosures": []
            }
            """;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryProductRepository inventoryProductRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = buildAuthenticatedMockMvc();
    }

    @Test
    void ornitorrincoShouldCrossCrudReportsAndPdfExportsInOneGo() throws Exception {
        int recentDaysWindow = Math.max(1, (int) ChronoUnit.DAYS.between(BASE_DATE, LocalDate.now()) + 1);

        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value(""));

        mockMvc.perform(put("/api/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SETTINGS_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Ornitorrinco Spa"))
                .andExpect(jsonPath("$.openingHours", hasSize(7)));

        Long serviceCategoryId = createCategory("Servicios Ornitorrinco");
        Long productCategoryId = createCategory("Productos Ornitorrinco");
        Long walkInClientId = createClient("Al contado", "ornitorrinco-caja@test.com");
        Long clientId = createClient(unique("Cliente Ornitorrinco"), "ornitorrinco-cliente@test.com");
        Long professionalId = createProfessional("Lara Vapor", true);
        Long serviceId = createService("Masaje Ornitorrinco", serviceCategoryId, 60, 80.00);
        Long productId = createInventoryProduct("Aceite Ornitorrinco", productCategoryId, 0, 2, null);

        Long supplyExpenseId = createExpense(
                BASE_DATE.minusDays(1),
                "Aceite Ornitorrinco",
                "SUPPLIES",
                5,
                "8.00",
                "Reposicion base"
        );

        Long otherExpenseId = createExpense(
                BASE_DATE,
                "Publicidad Ornitorrinco",
                "OTHER",
                1,
                "20.00",
                "Campana puntual"
        );

        Long appointmentId = createAppointment(
                clientId,
                serviceId,
                professionalId,
                BASE_DATE,
                BASE_TIME,
                "PENDING"
        );

        mockMvc.perform(put("/api/appointments/{id}", appointmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "serviceId": %d,
                                  "price": 80.0,
                                  "professionalId": %d,
                                  "appointmentDate": "%s",
                                  "appointmentTime": "%s",
                                  "status": "CONFIRMED",
                                  "internalNotes": "Cita ya confirmada"
                                }
                                """.formatted(clientId, serviceId, professionalId, BASE_DATE, BASE_TIME.plusHours(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.professionalId").value(professionalId.intValue()))
                .andExpect(jsonPath("$.professionalName").value("Lara Vapor"));

        Long manualSaleId = extractId(
                mockMvc.perform(post("/api/sales")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "itemType": "PRODUCT",
                                          "inventoryProductId": %d,
                                          "unitPrice": 15.00,
                                          "quantity": 2,
                                          "saleDate": "%s",
                                          "notes": "Venta sin cita"
                                        }
                                        """.formatted(productId, BASE_DATE)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.clientId").value(walkInClientId.intValue()))
                        .andExpect(jsonPath("$.clientName").value("Al contado"))
                        .andExpect(jsonPath("$.itemName").value("Aceite Ornitorrinco"))
                        .andExpect(jsonPath("$.totalAmount").value(30.0))
                        .andReturn()
        );

        assertThat(getStock(productId)).isEqualTo(3);
        assertThat(saleRepository.findByAppointmentId(appointmentId)).isPresent();
        assertThat(expenseRepository.findById(supplyExpenseId)).isPresent();
        assertThat(expenseRepository.findById(otherExpenseId)).isPresent();

        mockMvc.perform(get("/api/clients")
                        .param("search", "ornitorrinco-cliente")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(clientId.intValue()));

        mockMvc.perform(get("/api/professionals")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(professionalId.intValue()));

        mockMvc.perform(get("/api/services")
                        .param("search", "Masaje Ornitorrinco")
                        .param("active", "true")
                        .param("categoryId", String.valueOf(serviceCategoryId))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(serviceId.intValue()));

        mockMvc.perform(get("/api/inventory/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(3))
                .andExpect(jsonPath("$.status").value("IN_STOCK"));

        mockMvc.perform(get("/api/inventory/products")
                        .param("search", "Aceite Ornitorrinco")
                        .param("categoryIds", String.valueOf(productCategoryId))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(productId.intValue()))
                .andExpect(jsonPath("$.content[0].stock").value(3));

        mockMvc.perform(get("/api/appointments")
                        .param("fromDate", BASE_DATE.minusDays(1).toString())
                        .param("toDate", BASE_DATE.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(appointmentId.intValue()))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));

        mockMvc.perform(get("/api/sales")
                        .param("fromDate", BASE_DATE.toString())
                        .param("toDate", BASE_DATE.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(manualSaleId.intValue()))
                .andExpect(jsonPath("$.content[0].itemType").value("PRODUCT"));

        mockMvc.perform(get("/api/sales/recent")
                        .param("days", String.valueOf(recentDaysWindow))
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(manualSaleId.intValue()));

        mockMvc.perform(get("/api/expenses/recent")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].concept").value("Publicidad Ornitorrinco"));

        mockMvc.perform(get("/api/reports/monthly/simple")
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(110.0))
                .andExpect(jsonPath("$.totalExpenses").value(60.0))
                .andExpect(jsonPath("$.totalConfirmedAppointments").value(1))
                .andExpect(jsonPath("$.incomeChangePercentage").value(nullValue()))
                .andExpect(jsonPath("$.lastThreeMonths", hasSize(3)))
                .andExpect(jsonPath("$.lastThreeMonths[0].period").value("2026-02"))
                .andExpect(jsonPath("$.lastThreeMonths[0].income").value(0.0))
                .andExpect(jsonPath("$.lastThreeMonths[1].period").value("2026-03"))
                .andExpect(jsonPath("$.lastThreeMonths[2].period").value("2026-04"))
                .andExpect(jsonPath("$.lastThreeMonths[2].income").value(110.0))
                .andExpect(jsonPath("$.topServices", hasSize(1)))
                .andExpect(jsonPath("$.topServices[0].id").value(serviceId.intValue()))
                .andExpect(jsonPath("$.topServices[0].generatedIncome").value(80.0))
                .andExpect(jsonPath("$.busiestDays", hasSize(3)))
                .andExpect(jsonPath("$.busiestDays[0].dayOfWeek").value("WEDNESDAY"))
                .andExpect(jsonPath("$.busiestDays[0].confirmedAppointments").value(1))
                .andExpect(jsonPath("$.busiestDays[0].percentage").value(100.0));

        mockMvc.perform(get("/api/reports/monthly/detailed")
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossIncome").value(110.0))
                .andExpect(jsonPath("$.grossExpenses").value(60.0))
                .andExpect(jsonPath("$.operatingProfit").value(50.0))
                .andExpect(jsonPath("$.estimatedVatPayable").value(8.68))
                .andExpect(jsonPath("$.estimatedIrpfPayable").value(6.20))
                .andExpect(jsonPath("$.estimatedTotalTaxes").value(14.88))
                .andExpect(jsonPath("$.profitabilityRatio").value(45.45));

        mockMvc.perform(get("/api/reports/quarterly/detailed")
                        .param("year", "2026")
                        .param("quarter", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossIncome").value(110.0))
                .andExpect(jsonPath("$.grossExpenses").value(60.0))
                .andExpect(jsonPath("$.operatingProfit").value(50.0))
                .andExpect(jsonPath("$.operatingProfitChangePercentage").value(nullValue()))
                .andExpect(jsonPath("$.monthlyBreakdown", hasSize(3)))
                .andExpect(jsonPath("$.monthlyBreakdown[0].month").value(4))
                .andExpect(jsonPath("$.monthlyBreakdown[0].grossIncome").value(110.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].grossExpenses").value(60.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].operatingProfit").value(50.0))
                .andExpect(jsonPath("$.monthlyBreakdown[1].month").value(5))
                .andExpect(jsonPath("$.monthlyBreakdown[1].grossIncome").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[2].month").value(6))
                .andExpect(jsonPath("$.profitEvolution", hasSize(3)));

        mockMvc.perform(get("/api/reports/annual/consolidated")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossIncome").value(110.0))
                .andExpect(jsonPath("$.grossExpenses").value(60.0))
                .andExpect(jsonPath("$.operatingProfit").value(50.0))
                .andExpect(jsonPath("$.operatingProfitChangePercentage").value(nullValue()))
                .andExpect(jsonPath("$.monthlyBreakdown", hasSize(12)))
                .andExpect(jsonPath("$.monthlyBreakdown[3].month").value(4))
                .andExpect(jsonPath("$.monthlyBreakdown[3].grossIncome").value(110.0))
                .andExpect(jsonPath("$.monthlyBreakdown[3].grossExpenses").value(60.0))
                .andExpect(jsonPath("$.monthlyBreakdown[3].operatingProfit").value(50.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].grossIncome").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[11].grossIncome").value(0.0))
                .andExpect(jsonPath("$.profitEvolution", hasSize(12)));

        mockMvc.perform(get("/api/reports/ranking")
                        .param("type", "SERVICE")
                        .param("year", "2026")
                        .param("month", "4")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].position").value(1))
                .andExpect(jsonPath("$.content[0].id").value(serviceId.intValue()))
                .andExpect(jsonPath("$.content[0].generatedIncome").value(80.0));

        mockMvc.perform(get("/api/reports/ranking")
                        .param("type", "PRODUCT")
                        .param("year", "2026")
                        .param("month", "4")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].position").value(1))
                .andExpect(jsonPath("$.content[0].id").value(productId.intValue()))
                .andExpect(jsonPath("$.content[0].totalCount").value(2))
                .andExpect(jsonPath("$.content[0].generatedIncome").value(30.0))
                .andExpect(jsonPath("$.content[0].averageDurationMinutes").value(nullValue()));

        mockMvc.perform(get("/api/reports/ranking")
                        .param("type", "SERVICE")
                        .param("year", "2026")
                        .param("month", "4")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));

        assertPdfResponse(
                mockMvc.perform(get("/api/reports/monthly/detailed/pdf")
                                .param("year", "2026")
                                .param("month", "4"))
                        .andExpect(status().isOk())
                        .andReturn(),
                "informe-mensual-detallado-2026-04.pdf"
        );

        assertPdfResponse(
                mockMvc.perform(get("/api/reports/quarterly/detailed/pdf")
                                .param("year", "2026")
                                .param("quarter", "2"))
                        .andExpect(status().isOk())
                        .andReturn(),
                "informe-trimestral-detallado-2026-Q2.pdf"
        );

        assertPdfResponse(
                mockMvc.perform(get("/api/reports/annual/consolidated/pdf")
                                .param("year", "2026"))
                        .andExpect(status().isOk())
                        .andReturn(),
                "informe-anual-consolidado-2026.pdf"
        );

        assertPdfResponse(
                mockMvc.perform(get("/api/inventory/products/export/pdf")
                                .param("search", "Aceite Ornitorrinco"))
                        .andExpect(status().isOk())
                        .andReturn(),
                "inventory-report"
        );

        assertPdfResponse(
                mockMvc.perform(get("/api/sales/export/pdf")
                                .param("fromDate", BASE_DATE.toString())
                                .param("toDate", BASE_DATE.toString()))
                        .andExpect(status().isOk())
                        .andReturn(),
                "sales-report-" + BASE_DATE + "-to-" + BASE_DATE + ".pdf"
        );

        assertPdfResponse(
                mockMvc.perform(get("/api/expenses/export/pdf")
                                .param("fromDate", BASE_DATE.minusDays(1).toString())
                                .param("toDate", BASE_DATE.toString()))
                        .andExpect(status().isOk())
                        .andReturn(),
                "expenses-report"
        );
    }

    @Test
    void ornitorrincoShouldRewindInventoryAndAppointmentSideEffects() throws Exception {
        Long categoryId = createCategory("Reversibles Ornitorrinco");
        Long clientId = createClient(unique("Cliente Reversible"), "reversible@test.com");
        createClient("Al contado", "reversible-caja@test.com");
        Long serviceId = createService("Ritual Reversible", categoryId, 45, 55.00);

        InventoryProductEntity product = saveProduct("Bruma Reversible", 10);

        Long expenseId = createExpense(
                BASE_DATE,
                "Bruma Reversible",
                "SUPPLIES",
                4,
                "6.00",
                "Suma stock"
        );
        assertThat(getStock(product.getId())).isEqualTo(14);

        mockMvc.perform(put("/api/expenses/{id}", expenseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expenseDate": "%s",
                                  "concept": "Mudanza de almacen",
                                  "category": "OTHER",
                                  "quantity": 1,
                                  "amount": 24.00,
                                  "notes": "Ya no toca inventario"
                                }
                                """.formatted(BASE_DATE.plusDays(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("OTHER"))
                .andExpect(jsonPath("$.concept").value("Mudanza de almacen"));

        assertThat(getStock(product.getId())).isEqualTo(10);

        mockMvc.perform(delete("/api/expenses/{id}", expenseId))
                .andExpect(status().isNoContent());

        assertThat(getStock(product.getId())).isEqualTo(10);

        Long saleId = extractId(
                mockMvc.perform(post("/api/sales")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "clientId": %d,
                                          "itemType": "PRODUCT",
                                          "inventoryProductId": %d,
                                          "unitPrice": 9.50,
                                          "quantity": 3,
                                          "saleDate": "%s",
                                          "notes": "Resta stock"
                                        }
                                        """.formatted(clientId, product.getId(), BASE_DATE)))
                        .andExpect(status().isCreated())
                        .andReturn()
        );

        assertThat(getStock(product.getId())).isEqualTo(7);

        mockMvc.perform(put("/api/sales/{id}", saleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "itemType": "PRODUCT",
                                  "inventoryProductId": %d,
                                  "unitPrice": 9.50,
                                  "quantity": 1,
                                  "saleDate": "%s",
                                  "notes": "Ahora resta menos"
                                }
                                """.formatted(clientId, product.getId(), BASE_DATE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(1));

        assertThat(getStock(product.getId())).isEqualTo(9);

        mockMvc.perform(delete("/api/sales/{id}", saleId))
                .andExpect(status().isNoContent());

        assertThat(getStock(product.getId())).isEqualTo(10);

        Long appointmentId = extractId(
                mockMvc.perform(post("/api/appointments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "clientId": %d,
                                          "serviceId": %d,
                                          "price": 55.0,
                                          "professionalName": "Texto libre",
                                          "appointmentDate": "%s",
                                          "appointmentTime": "%s",
                                          "status": "PENDING",
                                          "internalNotes": "Primero pendiente"
                                        }
                                        """.formatted(clientId, serviceId, BASE_DATE, BASE_TIME.plusHours(2))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.professionalId").value(nullValue()))
                        .andExpect(jsonPath("$.professionalName").value("Texto libre"))
                        .andReturn()
        );

        mockMvc.perform(put("/api/appointments/{id}", appointmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "serviceId": %d,
                                  "price": 55.0,
                                  "professionalName": "Texto libre",
                                  "appointmentDate": "%s",
                                  "appointmentTime": "%s",
                                  "status": "CONFIRMED",
                                  "internalNotes": "Ya confirmada"
                                }
                                """.formatted(clientId, serviceId, BASE_DATE, BASE_TIME.plusHours(3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        SaleEntity appointmentSale = saleRepository.findByAppointmentId(appointmentId)
                .orElseThrow();
        assertThat(appointmentSale.getQuantity()).isEqualTo(1);

        mockMvc.perform(put("/api/appointments/{id}", appointmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "serviceId": %d,
                                  "price": 55.0,
                                  "professionalName": "Texto libre",
                                  "appointmentDate": "%s",
                                  "appointmentTime": "%s",
                                  "status": "CANCELLED",
                                  "internalNotes": "Se cayo"
                                }
                                """.formatted(clientId, serviceId, BASE_DATE, BASE_TIME.plusHours(4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(saleRepository.findByAppointmentId(appointmentId)).isEmpty();

        mockMvc.perform(delete("/api/appointments/{id}", appointmentId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/appointments/{id}", appointmentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void ornitorrincoShouldRejectRealisticBadIdeas() throws Exception {
        Long categoryId = createCategory("Conflictos Ornitorrinco");
        Long serviceId = createService("Servicio Conflictivo", categoryId, 30, 25.00);
        Long professionalId = createProfessional("Profesional Dormido", false);
        Long clientId = createClient(unique("Cliente Conflictivo"), "cliente-conflictivo@test.com");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "conflictos ornitorrinco",
                                  "color": "red",
                                  "icon": "alert"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemType": "SERVICE",
                                  "serviceId": %d,
                                  "quantity": 1,
                                  "saleDate": "%s"
                                }
                                """.formatted(serviceId, BASE_DATE)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "serviceId": %d,
                                  "price": 25.0,
                                  "professionalId": %d,
                                  "appointmentDate": "%s",
                                  "appointmentTime": "%s",
                                  "status": "PENDING",
                                  "internalNotes": "Debe fallar por profesional inactivo"
                                }
                                """.formatted(clientId, serviceId, professionalId, BASE_DATE, BASE_TIME)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(post("/api/inventory/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Producto Malparido",
                                  "description": "No deberia entrar",
                                  "categoryId": %d,
                                  "stock": 1,
                                  "minimumStock": 0,
                                  "sku": "SKU-%s",
                                  "purchasePrice": null
                                }
                                """.formatted(categoryId, UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expenseDate": "%s",
                                  "concept": "Producto fantasma",
                                  "category": "SUPPLIES",
                                  "quantity": 2,
                                  "amount": 12.00,
                                  "notes": "No existe en inventario"
                                }
                                """.formatted(BASE_DATE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/reports/ranking")
                        .param("type", "SERVICE")
                        .param("year", "2026")
                        .param("month", "4")
                        .param("page", "0")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/reports/annual/consolidated")
                        .param("year", "1900"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/clients/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(get("/api/sales")
                        .param("fromDate", BASE_DATE.plusDays(1).toString())
                        .param("toDate", BASE_DATE.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    private Long createCategory(String name) throws Exception {
        return extractId(
                mockMvc.perform(post("/api/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "%s",
                                          "color": "blue",
                                          "icon": "tag"
                                        }
                                        """.formatted(name)))
                        .andExpect(status().isCreated())
                        .andReturn()
        );
    }

    private Long createClient(String fullName, String email) throws Exception {
        return extractId(
                mockMvc.perform(post("/api/clients")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fullName": "%s",
                                          "phone": "600111222",
                                          "email": "%s"
                                        }
                                        """.formatted(fullName, email)))
                        .andExpect(status().isCreated())
                        .andReturn()
        );
    }

    private Long createProfessional(String fullName, boolean active) throws Exception {
        return extractId(
                mockMvc.perform(post("/api/professionals")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fullName": "%s",
                                          "specialty": "Tecnica",
                                          "phone": "600222333",
                                          "email": "%s",
                                          "accessPassword": "clave-profesional",
                                          "active": %s
                                        }
                                        """.formatted(fullName, UUID.randomUUID() + "@test.com", active)))
                        .andExpect(status().isCreated())
                        .andReturn()
        );
    }

    private Long createService(String name, Long categoryId, int durationMinutes, double price) throws Exception {
        String priceJson = BigDecimal.valueOf(price).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();

        return extractId(
                mockMvc.perform(post("/api/services")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "%s",
                                          "description": "Servicio de prueba",
                                          "durationMinutes": %d,
                                          "price": %s,
                                          "active": true,
                                          "categoryId": %d
                                        }
                                        """.formatted(name, durationMinutes, priceJson, categoryId)))
                        .andExpect(status().isCreated())
                        .andReturn()
        );
    }

    private Long createInventoryProduct(
            String name,
            Long categoryId,
            int stock,
            int minimumStock,
            BigDecimal purchasePrice
    ) throws Exception {
        String purchasePriceJson = purchasePrice == null ? "null" : purchasePrice.toPlainString();

        return extractId(
                mockMvc.perform(post("/api/inventory/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "%s",
                                          "description": "Producto de prueba",
                                          "categoryId": %d,
                                          "stock": %d,
                                          "minimumStock": %d,
                                          "sku": "SKU-%s",
                                          "purchasePrice": %s
                                        }
                                        """.formatted(name, categoryId, stock, minimumStock, UUID.randomUUID(), purchasePriceJson)))
                        .andExpect(status().isOk())
                        .andReturn()
        );
    }

    private Long createExpense(
            LocalDate expenseDate,
            String concept,
            String category,
            int quantity,
            String amount,
            String notes
    ) throws Exception {
        return extractId(
                mockMvc.perform(post("/api/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "expenseDate": "%s",
                                          "concept": "%s",
                                          "category": "%s",
                                          "quantity": %d,
                                          "amount": %s,
                                          "notes": "%s"
                                        }
                                        """.formatted(expenseDate, concept, category, quantity, amount, notes)))
                        .andExpect(status().isOk())
                        .andReturn()
        );
    }

    private Long createAppointment(
            Long clientId,
            Long serviceId,
            Long professionalId,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            String status
    ) throws Exception {
        return extractId(
                mockMvc.perform(post("/api/appointments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "clientId": %d,
                                          "serviceId": %d,
                                          "price": 80.0,
                                          "professionalId": %d,
                                          "appointmentDate": "%s",
                                          "appointmentTime": "%s",
                                          "status": "%s",
                                          "internalNotes": "Cita del ornitorrinco"
                                        }
                                        """.formatted(clientId, serviceId, professionalId, appointmentDate, appointmentTime, status)))
                        .andExpect(status().isOk())
                        .andReturn()
        );
    }

    private void assertPdfResponse(MvcResult result, String expectedFileNameFragment) throws Exception {
        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 4)).isEqualTo("%PDF");
        assertThat(result.getResponse().getContentType()).contains(MediaType.APPLICATION_PDF_VALUE);
        assertThat(result.getResponse().getHeader("Content-Disposition")).contains(expectedFileNameFragment);
    }

    private Long extractId(MvcResult result) throws Exception {
        Matcher matcher = ID_PATTERN.matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new IllegalStateException("No se pudo extraer el id: " + result.getResponse().getContentAsString());
        }
        return Long.parseLong(matcher.group(1));
    }

    private int getStock(Long productId) {
        return inventoryProductRepository.findById(productId)
                .orElseThrow()
                .getStock();
    }

    private InventoryProductEntity saveProduct(String name, int stock) {
        CategoryEntity category = categoryRepository.saveAndFlush(
                CategoryEntity.builder()
                        .name(unique("Categoria " + name))
                        .color("green")
                        .icon("drop")
                        .build()
        );

        return inventoryProductRepository.saveAndFlush(
                InventoryProductEntity.builder()
                        .name(name)
                        .description("Producto persistido para pruebas")
                        .category(category)
                        .stock(stock)
                        .minimumStock(2)
                        .sku("SKU-" + UUID.randomUUID())
                        .purchasePrice(new BigDecimal("5.00"))
                        .active(true)
                        .build()
        );
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
