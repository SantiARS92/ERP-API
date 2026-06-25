package es.supplesoftware.autonomos_api.common;

import es.supplesoftware.autonomos_api.categories.infrastructure.CategoryEntity;
import es.supplesoftware.autonomos_api.clients.infrastructure.ClientEntity;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductEntity;
import es.supplesoftware.autonomos_api.professionals.infrastructure.ProfessionalEntity;
import es.supplesoftware.autonomos_api.services.infrastructure.ServiceEntity;
import es.supplesoftware.autonomos_api.categories.infrastructure.CategoryRepository;
import es.supplesoftware.autonomos_api.clients.infrastructure.ClientRepository;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductRepository;
import es.supplesoftware.autonomos_api.professionals.infrastructure.ProfessionalRepository;
import es.supplesoftware.autonomos_api.services.infrastructure.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.hasSize;
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
class RoleAuthorizationApiIntegrationTest extends AuthenticatedApiIntegrationTestSupport {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
    private static final LocalDate BASE_DATE = LocalDate.of(2026, 4, 9);
    private static final LocalTime BASE_TIME = LocalTime.of(10, 30);

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private InventoryProductRepository inventoryProductRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildBaseMockMvc();
    }

    @Test
    void guestCanReadFrontendDashboardAndAgendaEndpoints() throws Exception {
        String guestToken = issueGuestToken();

        mockMvc.perform(get("/api/appointments")
                        .param("fromDate", BASE_DATE.toString())
                        .param("toDate", BASE_DATE.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/clients")
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/services")
                        .param("size", "100")
                        .param("active", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/professionals")
                        .param("active", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/inventory/products")
                        .param("size", "1000")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void targetReadEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/inventory/products"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/inventory/products/{id}", 1))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/professionals"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/professionals/{id}", 1))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void guestCannotWriteAppointments() throws Exception {
        String guestToken = issueGuestToken();

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/appointments/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/appointments/{id}", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void guestCannotAccessRestrictedModules() throws Exception {
        String guestToken = issueGuestToken();

        mockMvc.perform(get("/api/reports/monthly/simple")
                        .param("year", "2026")
                        .param("month", "4")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/settings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/sales")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/expenses")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/professionals/{id}", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void expenseEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/expenses")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/expenses/{id}", 1))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/expenses/recent")
                        .param("limit", "5"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/expenses/suggestions")
                        .param("query", "auth")
                        .param("category", "OTHER"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("Gasto sin token")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/expenses/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("Gasto sin token editado")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/expenses/{id}", 1))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/expenses/export/pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void guestCannotAccessAnyExpenseEndpointWithJwt() throws Exception {
        String guestToken = issueGuestToken();

        mockMvc.perform(get("/api/expenses")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/expenses/{id}", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/expenses/recent")
                        .param("limit", "5")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/expenses/suggestions")
                        .param("query", "auth")
                        .param("category", "OTHER")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("Gasto guest"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/expenses/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("Gasto guest editado"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/expenses/{id}", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/expenses/export/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void professionalCanManageAppointmentsCrud() throws Exception {
        String professionalToken = issueProfessionalToken("Profesional Citas", "clave-citas");
        CategoryEntity category = saveCategory("Servicios Citas");
        ClientEntity client = saveClient("Cliente Citas");
        ServiceEntity service = saveService("Tratamiento Citas", 55.0, category);

        Long appointmentId = extractId(
                mockMvc.perform(post("/api/appointments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "clientId": %d,
                                          "serviceId": %d,
                                          "price": 55.0,
                                          "professionalName": "Profesional Citas",
                                          "appointmentDate": "%s",
                                          "appointmentTime": "%s",
                                          "status": "PENDING",
                                          "internalNotes": "Creada por profesional"
                                        }
                                        """.formatted(client.getId(), service.getId(), BASE_DATE, BASE_TIME))
                                .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                        .andExpect(status().isOk())
                        .andReturn()
        );

        mockMvc.perform(get("/api/appointments")
                        .param("fromDate", BASE_DATE.toString())
                        .param("toDate", BASE_DATE.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(appointmentId.intValue()));

        mockMvc.perform(get("/api/appointments/{id}", appointmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointmentId.intValue()));

        mockMvc.perform(put("/api/appointments/{id}", appointmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "serviceId": %d,
                                  "price": 55.0,
                                  "professionalName": "Profesional Citas",
                                  "appointmentDate": "%s",
                                  "appointmentTime": "%s",
                                  "status": "CONFIRMED",
                                  "internalNotes": "Actualizada por profesional"
                                }
                                """.formatted(client.getId(), service.getId(), BASE_DATE, BASE_TIME.plusHours(1)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(delete("/api/appointments/{id}", appointmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk());
    }

    @Test
    void professionalCanUseClientsServicesSalesAndExpenses() throws Exception {
        String professionalToken = issueProfessionalToken("Profesional Modulos", "clave-modulos");
        CategoryEntity category = saveCategory("Categoria Modulos");
        InventoryProductEntity product = saveProduct("Producto Modulos", 20, category);

        Long clientId = extractId(
                mockMvc.perform(post("/api/clients")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fullName": "%s",
                                          "phone": "600111222",
                                          "email": "%s"
                                        }
                                        """.formatted(unique("Cliente Profesional"), unique("cliente") + "@test.com"))
                                .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                        .andExpect(status().isCreated())
                        .andReturn()
        );

        mockMvc.perform(get("/api/clients")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(put("/api/clients/{id}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Cliente Profesional Editado",
                                  "phone": "600333444",
                                  "email": "cliente-editado@test.com"
                                }
                                """)
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clientId.intValue()));

        Long serviceId = extractId(
                mockMvc.perform(post("/api/services")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "%s",
                                          "description": "Servicio profesional",
                                          "durationMinutes": 45,
                                          "price": 35.0,
                                          "active": true,
                                          "categoryId": %d
                                        }
                                        """.formatted(unique("Servicio Profesional"), category.getId()))
                                .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                        .andExpect(status().isCreated())
                        .andReturn()
        );

        mockMvc.perform(get("/api/services")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(put("/api/services/{id}", serviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Servicio Profesional Editado",
                                  "description": "Servicio actualizado",
                                  "durationMinutes": 50,
                                  "price": 40.0,
                                  "active": true,
                                  "categoryId": %d
                                }
                                """.formatted(category.getId()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceId.intValue()));

        Long saleId = extractId(
                mockMvc.perform(post("/api/sales")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "clientId": %d,
                                          "itemType": "PRODUCT",
                                          "inventoryProductId": %d,
                                          "unitPrice": 12.50,
                                          "quantity": 2,
                                          "saleDate": "%s",
                                          "notes": "Venta profesional"
                                        }
                                        """.formatted(clientId, product.getId(), BASE_DATE))
                                .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                        .andExpect(status().isCreated())
                        .andReturn()
        );

        mockMvc.perform(get("/api/sales")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(put("/api/sales/{id}", saleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": %d,
                                  "itemType": "PRODUCT",
                                  "inventoryProductId": %d,
                                  "unitPrice": 12.50,
                                  "quantity": 3,
                                  "saleDate": "%s",
                                  "notes": "Venta profesional editada"
                                }
                                """.formatted(clientId, product.getId(), BASE_DATE))
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saleId.intValue()));

        mockMvc.perform(delete("/api/sales/{id}", saleId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isNoContent());

        Long expenseId = extractId(
                mockMvc.perform(post("/api/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "expenseDate": "%s",
                                          "concept": "Publicidad profesional",
                                          "category": "OTHER",
                                          "quantity": 1,
                                          "amount": 35.00,
                                          "notes": "Gasto profesional"
                                        }
                                        """.formatted(BASE_DATE))
                                .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                        .andExpect(status().isOk())
                        .andReturn()
        );

        mockMvc.perform(get("/api/expenses")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(put("/api/expenses/{id}", expenseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expenseDate": "%s",
                                  "concept": "Publicidad profesional editada",
                                  "category": "OTHER",
                                  "quantity": 1,
                                  "amount": 45.00,
                                  "notes": "Gasto actualizado"
                                }
                                """.formatted(BASE_DATE.plusDays(1)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expenseId.intValue()));

        mockMvc.perform(delete("/api/expenses/{id}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void professionalCanUseInventoryExceptDelete() throws Exception {
        String professionalToken = issueProfessionalToken("Profesional Inventario", "clave-inventario");
        CategoryEntity category = saveCategory("Categoria Inventario");

        Long productId = extractId(
                mockMvc.perform(post("/api/inventory/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Producto Profesional",
                                          "description": "Producto creado por profesional",
                                          "categoryId": %d,
                                          "stock": 15,
                                          "minimumStock": 5,
                                          "sku": "SKU-%s",
                                          "purchasePrice": 18.50
                                        }
                                        """.formatted(category.getId(), UUID.randomUUID()))
                                .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                        .andExpect(status().isOk())
                        .andReturn()
        );

        mockMvc.perform(get("/api/inventory/products")
                        .param("search", "Producto Profesional")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/inventory/products/{id}", productId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId.intValue()));

        mockMvc.perform(put("/api/inventory/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Producto Profesional Editado",
                                  "description": "Producto actualizado por profesional",
                                  "categoryId": %d,
                                  "stock": 20,
                                  "minimumStock": 4,
                                  "sku": "SKU-%s",
                                  "purchasePrice": 19.50
                                }
                                """.formatted(category.getId(), UUID.randomUUID()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId.intValue()));

        mockMvc.perform(get("/api/inventory/products/export/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));

        mockMvc.perform(delete("/api/inventory/products/{id}", productId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void professionalCannotAccessAdminOnlyModules() throws Exception {
        String professionalToken = issueProfessionalToken("Profesional Restringido", "clave-restringida");

        mockMvc.perform(get("/api/reports/monthly/simple")
                        .param("year", "2026")
                        .param("month", "4")
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/settings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/professionals")
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/professionals/{id}", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAllExpenseEndpointsWithJwt() throws Exception {
        assertCanAccessAllExpenseEndpoints(issueAdminToken(), "Gasto Admin Auth");
    }

    @Test
    void professionalCanAccessAllExpenseEndpointsWithJwt() throws Exception {
        assertCanAccessAllExpenseEndpoints(
                issueProfessionalToken("Profesional Gastos", "clave-gastos"),
                "Gasto Professional Auth"
        );
    }

    @Test
    void authenticatedExpenseRequestValidationErrorsShouldNotBecomeUnauthorized() throws Exception {
        String adminToken = issueAdminToken();

        mockMvc.perform(get("/api/expenses")
                        .param("page", "-1")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void professionalCanReadCategoriesButCannotWriteThem() throws Exception {
        String professionalToken = issueProfessionalToken("Profesional Categorias", "clave-categorias");
        CategoryEntity category = saveCategory("Categoria Solo Lectura");

        mockMvc.perform(get("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/categories/{id}", category.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(category.getId().intValue()));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Nueva categoria",
                                  "color": "blue",
                                  "icon": "tag"
                                }
                                """)
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/categories/{id}", category.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Categoria editada",
                                  "color": "green",
                                  "icon": "leaf"
                                }
                                """)
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/categories/{id}", category.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(professionalToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminKeepsFullAccessToRestrictedModules() throws Exception {
        String adminToken = issueAdminToken();

        mockMvc.perform(get("/api/settings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/professionals")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/annual/consolidated")
                        .param("year", "2030")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "color": "blue",
                                  "icon": "tag"
                                }
                                """.formatted(unique("Categoria Admin")))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isCreated());
    }

    @Test
    void adminCanReadTargetInventoryAndProfessionalEndpointsWithJwt() throws Exception {
        String adminToken = issueAdminToken();
        ProfessionalEntity professional = saveProfessional("Profesional Lectura Admin", "clave-admin-lectura", true);
        CategoryEntity category = saveCategory("Categoria Lectura Admin");
        InventoryProductEntity product = saveProduct("Producto Lectura Admin", 8, category);

        mockMvc.perform(get("/api/inventory/products")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/inventory/products/{id}", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId().intValue()));

        mockMvc.perform(get("/api/professionals")
                        .param("active", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/professionals/{id}", professional.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(professional.getId().intValue()));
    }

    @Test
    void adminCanDeleteInventoryProducts() throws Exception {
        String adminToken = issueAdminToken();
        CategoryEntity category = saveCategory("Categoria Delete Admin");
        InventoryProductEntity product = saveProduct("Producto Delete Admin", 10, category);

        mockMvc.perform(delete("/api/inventory/products/{id}", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/inventory/products/{id}", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    private String issueProfessionalToken(String fullName, String rawPassword) throws Exception {
        saveProfessional(fullName, rawPassword, true);
        return issueLoginToken(fullName, rawPassword);
    }

    private ProfessionalEntity saveProfessional(String fullName, String rawPassword, boolean active) {
        ProfessionalEntity professional = new ProfessionalEntity();
        professional.setFullName(fullName);
        professional.setSpecialty("Tecnica");
        professional.setPhone("600000000");
        professional.setEmail(UUID.randomUUID() + "@test.com");
        professional.setPasswordHash(passwordEncoder.encode(rawPassword));
        professional.setActive(active);
        return professionalRepository.saveAndFlush(professional);
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

    private ServiceEntity saveService(String name, double price, CategoryEntity category) {
        return serviceRepository.saveAndFlush(
                ServiceEntity.builder()
                        .name(name)
                        .description("Servicio para autorizacion")
                        .durationMinutes(60)
                        .price(price)
                        .active(true)
                        .category(category)
                        .build()
        );
    }

    private InventoryProductEntity saveProduct(String name, int stock, CategoryEntity category) {
        return inventoryProductRepository.saveAndFlush(
                InventoryProductEntity.builder()
                        .name(name)
                        .description("Producto para autorizacion")
                        .category(category)
                        .stock(stock)
                        .minimumStock(3)
                        .sku("SKU-" + UUID.randomUUID())
                        .purchasePrice(new BigDecimal("10.00"))
                        .active(true)
                        .build()
        );
    }

    private Long extractId(MvcResult result) throws Exception {
        Matcher matcher = ID_PATTERN.matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new IllegalStateException("No se pudo extraer el id: " + result.getResponse().getContentAsString());
        }
        return Long.parseLong(matcher.group(1));
    }

    private void assertCanAccessAllExpenseEndpoints(String token, String conceptPrefix) throws Exception {
        String concept = unique(conceptPrefix);
        Long expenseId = extractId(
                mockMvc.perform(post("/api/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(expenseJson(concept))
                                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.concept").value(concept))
                        .andReturn()
        );

        mockMvc.perform(get("/api/expenses")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/expenses/{id}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expenseId.intValue()));

        mockMvc.perform(get("/api/expenses/recent")
                        .param("limit", "5")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/expenses/suggestions")
                        .param("query", conceptPrefix)
                        .param("category", "OTHER")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/expenses/export/pdf")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));

        String updatedConcept = concept + " editado";
        mockMvc.perform(put("/api/expenses/{id}", expenseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson(updatedConcept))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expenseId.intValue()))
                .andExpect(jsonPath("$.concept").value(updatedConcept));

        mockMvc.perform(delete("/api/expenses/{id}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
    }

    private String expenseJson(String concept) {
        return """
                {
                  "expenseDate": "%s",
                  "concept": "%s",
                  "category": "OTHER",
                  "quantity": 1,
                  "amount": 35.00,
                  "notes": "Gasto autorizacion"
                }
                """.formatted(BASE_DATE, concept);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
