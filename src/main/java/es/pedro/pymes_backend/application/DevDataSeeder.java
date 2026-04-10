package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.AppointmentStatus;
import es.pedro.pymes_backend.domain.ExpenseCategory;
import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.domain.PaymentMethod;
import es.pedro.pymes_backend.domain.SaleItemType;
import es.pedro.pymes_backend.infrastructure.repository.AppointmentRepository;
import es.pedro.pymes_backend.infrastructure.repository.CategoryRepository;
import es.pedro.pymes_backend.infrastructure.repository.ClientRepository;
import es.pedro.pymes_backend.infrastructure.repository.ExpenseRepository;
import es.pedro.pymes_backend.infrastructure.repository.InventoryProductRepository;
import es.pedro.pymes_backend.infrastructure.repository.ProfessionalRepository;
import es.pedro.pymes_backend.infrastructure.repository.SaleRepository;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.AppointmentRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.AppointmentResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.CategoryRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.CategoryResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.ClientRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.ClientResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateExpenseRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateInventoryProductRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateSaleRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.InventoryProductResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.ProfessionalRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.ProfessionalResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.ServiceRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.ServiceResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "demo-data", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private static final long RANDOM_SEED = 20260410L;
    private static final int TARGET_CLIENT_COUNT = 121;
    private static final int TARGET_APPOINTMENT_COUNT = 180;
    private static final int TARGET_MANUAL_SALES = 80;
    private static final int TARGET_SUPPLIES_EXPENSES = 18;
    private static final String WALK_IN_CLIENT_NAME = "Al contado";
    private static final String PROFESSIONAL_DEMO_PASSWORD = "1234";
    private static final List<PaymentMethod> PAYMENT_METHODS = Arrays.asList(PaymentMethod.values());
    private static final List<LocalTime> APPOINTMENT_TIME_SLOTS = buildTimeSlots(LocalTime.of(9, 0), LocalTime.of(19, 0), 30);
    private static final List<LocalTime> SALE_TIME_SLOTS = buildTimeSlots(LocalTime.of(9, 30), LocalTime.of(20, 0), 30);

    private static final List<CategorySeed> CATEGORY_SEEDS = List.of(
            new CategorySeed("Corte y Peinado", "rose", "scissors"),
            new CategorySeed("Coloracion", "amber", "palette"),
            new CategorySeed("Barberia", "slate", "shield"),
            new CategorySeed("Unas", "pink", "sparkles"),
            new CategorySeed("Estetica", "emerald", "flower"),
            new CategorySeed("Masajes", "teal", "leaf"),
            new CategorySeed("Retail", "indigo", "shopping-bag")
    );
    private static final List<ServiceSeed> SERVICE_SEEDS = List.of(
            new ServiceSeed("Corte femenino express", "Corte y definicion para mantenimiento habitual.", 45, 24.00, true, "Corte y Peinado"),
            new ServiceSeed("Corte caballero tradicional", "Corte clasico con acabado a tijera o maquina.", 30, 16.00, true, "Barberia"),
            new ServiceSeed("Corte infantil", "Servicio rapido para peques con acabado natural.", 30, 15.00, true, "Corte y Peinado"),
            new ServiceSeed("Lavado y brushing", "Lavado completo con secado pulido y movimiento.", 45, 22.00, true, "Corte y Peinado"),
            new ServiceSeed("Peinado ondas suaves", "Peinado con ondas abiertas para diario o evento.", 60, 28.00, true, "Corte y Peinado"),
            new ServiceSeed("Recogido novia prueba", "Prueba de recogido para eventos y bodas.", 120, 65.00, false, "Corte y Peinado"),
            new ServiceSeed("Color raiz", "Cobertura de raiz y uniformidad de tono.", 90, 39.00, true, "Coloracion"),
            new ServiceSeed("Bano de color brillo", "Refresco de color y brillo global.", 75, 43.00, true, "Coloracion"),
            new ServiceSeed("Mechas babylights", "Mecha fina para efecto luminoso natural.", 150, 98.00, true, "Coloracion"),
            new ServiceSeed("Balayage completo", "Tecnica de aclarado degradado con matiz final.", 180, 145.00, true, "Coloracion"),
            new ServiceSeed("Matiz express", "Correccion de reflejo o mantenimiento entre coloraciones.", 45, 27.00, true, "Coloracion"),
            new ServiceSeed("Ritual reparacion post color", "Tratamiento sellador y nutritivo tras quimico.", 45, 31.00, true, "Coloracion"),
            new ServiceSeed("Afeitado clasico", "Ritual de afeitado con toalla caliente y locion.", 30, 18.00, true, "Barberia"),
            new ServiceSeed("Arreglo de barba", "Perfilado, rebaje y forma de barba.", 30, 15.00, true, "Barberia"),
            new ServiceSeed("Corte fade premium", "Degradado tecnico con lavado y acabado.", 45, 24.00, true, "Barberia"),
            new ServiceSeed("Ritual barba toalla caliente", "Servicio premium de barberia para mantenimiento profundo.", 45, 26.00, false, "Barberia"),
            new ServiceSeed("Manicura semipermanente", "Preparacion, esmaltado y sellado de larga duracion.", 60, 26.00, true, "Unas"),
            new ServiceSeed("Pedicura spa", "Pedicura completa con exfoliacion e hidratacion.", 75, 34.00, true, "Unas"),
            new ServiceSeed("Retirada y refuerzo", "Retirada de producto anterior y refuerzo de una.", 45, 19.00, true, "Unas"),
            new ServiceSeed("Nail art premium", "Decoracion avanzada personalizada.", 60, 29.00, false, "Unas"),
            new ServiceSeed("Limpieza facial express", "Puesta a punto rapida para mantenimiento.", 40, 29.00, true, "Estetica"),
            new ServiceSeed("Higiene facial profunda", "Limpieza completa con extraccion y mascarilla.", 75, 49.00, true, "Estetica"),
            new ServiceSeed("Diseno de cejas", "Definicion de ceja con pinza y acabado.", 30, 18.00, true, "Estetica"),
            new ServiceSeed("Lifting de pestanas", "Curvado de pestanas con efecto natural.", 60, 38.00, true, "Estetica"),
            new ServiceSeed("Masaje relajante espalda", "Masaje de descarga y relajacion de espalda.", 45, 35.00, true, "Masajes"),
            new ServiceSeed("Masaje descontracturante completo", "Trabajo profundo de cuello, espalda y piernas.", 60, 45.00, true, "Masajes")
    );
    private static final List<ProfessionalSeed> PROFESSIONAL_SEEDS = List.of(
            new ProfessionalSeed("Lucia Torres", "Colorista senior", "611220301", "lucia.torres@demo-pymes.es", true),
            new ProfessionalSeed("Marina Vega", "Tecnica en unas", "611220302", "marina.vega@demo-pymes.es", true),
            new ProfessionalSeed("Sergio Lozano", "Barbero", "611220303", "sergio.lozano@demo-pymes.es", true),
            new ProfessionalSeed("Clara Benitez", "Esteticista facial", "611220304", "clara.benitez@demo-pymes.es", true),
            new ProfessionalSeed("Paula Rios", "Masajista", "611220305", "paula.rios@demo-pymes.es", false)
    );
    private static final List<InventoryTemplate> INVENTORY_TEMPLATES = List.of(
            new InventoryTemplate("Champu reparador keratina pro", "Retail", money("7.80")),
            new InventoryTemplate("Champu volumen raiz ligera", "Retail", money("7.40")),
            new InventoryTemplate("Champu color shield intenso", "Retail", money("8.10")),
            new InventoryTemplate("Acondicionador hidratante argan balance", "Retail", money("8.60")),
            new InventoryTemplate("Acondicionador rizos control suave", "Retail", money("8.20")),
            new InventoryTemplate("Mascarilla nutricion seda", "Retail", money("9.90")),
            new InventoryTemplate("Mascarilla detox cuero cabelludo", "Retail", money("10.20")),
            new InventoryTemplate("Serum puntas sellado gloss", "Retail", money("11.40")),
            new InventoryTemplate("Protector termico daily smooth", "Retail", money("8.90")),
            new InventoryTemplate("Spray brillo instantaneo mirror", "Retail", money("8.50")),
            new InventoryTemplate("Mousse volumen flexible", "Retail", money("6.80")),
            new InventoryTemplate("Laca fijacion media studio", "Retail", money("6.50")),
            new InventoryTemplate("Tinte permanente 4.0 castano", "Coloracion", money("4.90")),
            new InventoryTemplate("Tinte permanente 6.34 miel", "Coloracion", money("5.10")),
            new InventoryTemplate("Tinte permanente 7.1 ceniza", "Coloracion", money("5.10")),
            new InventoryTemplate("Oxidante crema 20 vol", "Coloracion", money("3.60")),
            new InventoryTemplate("Oxidante crema 30 vol", "Coloracion", money("3.80")),
            new InventoryTemplate("Decolorante polvo azul", "Coloracion", money("12.50")),
            new InventoryTemplate("Matizador perla frio", "Coloracion", money("6.90")),
            new InventoryTemplate("Plex reparador tecnico", "Coloracion", money("14.80")),
            new InventoryTemplate("Removedor de color suave", "Coloracion", money("11.70")),
            new InventoryTemplate("Champu post color ph balance", "Coloracion", money("8.40")),
            new InventoryTemplate("Aceite de barba cedar", "Barberia", money("6.20")),
            new InventoryTemplate("Balsamo after shave sensitive", "Barberia", money("5.90")),
            new InventoryTemplate("Cera mate barber craft", "Barberia", money("6.80")),
            new InventoryTemplate("Pomada brillo clasico", "Barberia", money("6.60")),
            new InventoryTemplate("Gel de afeitado transparente", "Barberia", money("7.10")),
            new InventoryTemplate("Toallas comprimidas premium", "Barberia", money("4.20")),
            new InventoryTemplate("Talco cuello fresh cut", "Barberia", money("3.90")),
            new InventoryTemplate("Tonic cuero cabelludo mint", "Barberia", money("5.40")),
            new InventoryTemplate("Gel limpiador vitamina c", "Estetica", money("9.60")),
            new InventoryTemplate("Espuma purificante facial", "Estetica", money("8.90")),
            new InventoryTemplate("Exfoliante enzimatico suave", "Estetica", money("10.80")),
            new InventoryTemplate("Tonico calmante aloe", "Estetica", money("7.40")),
            new InventoryTemplate("Serum acido hialuronico", "Estetica", money("14.50")),
            new InventoryTemplate("Crema nutritiva cabina", "Estetica", money("11.20")),
            new InventoryTemplate("Mascara arcilla purificante", "Estetica", money("9.30")),
            new InventoryTemplate("Contorno de ojos cafeina", "Estetica", money("8.70")),
            new InventoryTemplate("Ampollas glow flash", "Estetica", money("12.80")),
            new InventoryTemplate("Protector solar facial spf50", "Estetica", money("9.80")),
            new InventoryTemplate("Base rubber nude", "Unas", money("6.30")),
            new InventoryTemplate("Top coat brillo espejo", "Unas", money("6.10")),
            new InventoryTemplate("Esmalte semipermanente rojo cereza", "Unas", money("5.40")),
            new InventoryTemplate("Esmalte semipermanente arena rose", "Unas", money("5.40")),
            new InventoryTemplate("Aceite de cuticulas vainilla", "Unas", money("4.60")),
            new InventoryTemplate("Primer sin acido", "Unas", money("4.90")),
            new InventoryTemplate("Gel constructor cover", "Unas", money("8.40")),
            new InventoryTemplate("Removedor soak off pro", "Unas", money("5.10"))
    );
    private static final List<ExpenseConceptSeed> SERVICES_EXPENSE_SEEDS = List.of(
            new ExpenseConceptSeed("Gestoria laboral", ExpenseCategory.SERVICES, 1, money("180.00")),
            new ExpenseConceptSeed("Mantenimiento TPV", ExpenseCategory.SERVICES, 1, money("59.00")),
            new ExpenseConceptSeed("Servicio de limpieza", ExpenseCategory.SERVICES, 2, money("220.00")),
            new ExpenseConceptSeed("Lavanderia de toallas", ExpenseCategory.SERVICES, 4, money("68.00")),
            new ExpenseConceptSeed("Suscripcion agenda online", ExpenseCategory.SERVICES, 1, money("49.00")),
            new ExpenseConceptSeed("Reparacion secador profesional", ExpenseCategory.SERVICES, 1, money("95.00")),
            new ExpenseConceptSeed("Campana anuncios instagram", ExpenseCategory.SERVICES, 1, money("120.00")),
            new ExpenseConceptSeed("Internet y telefonia", ExpenseCategory.SERVICES, 1, money("72.00")),
            new ExpenseConceptSeed("Formacion colorimetria", ExpenseCategory.SERVICES, 2, money("260.00")),
            new ExpenseConceptSeed("Mantenimiento aire acondicionado", ExpenseCategory.SERVICES, 1, money("140.00")),
            new ExpenseConceptSeed("Servicio fotografico redes", ExpenseCategory.SERVICES, 1, money("175.00")),
            new ExpenseConceptSeed("Control de plagas preventivo", ExpenseCategory.SERVICES, 1, money("85.00"))
    );
    private static final List<ExpenseConceptSeed> OTHER_EXPENSE_SEEDS = List.of(
            new ExpenseConceptSeed("Alquiler local", ExpenseCategory.OTHER, 1, money("1450.00")),
            new ExpenseConceptSeed("Seguro responsabilidad civil", ExpenseCategory.OTHER, 1, money("230.00")),
            new ExpenseConceptSeed("Electricidad mensual", ExpenseCategory.OTHER, 1, money("310.00")),
            new ExpenseConceptSeed("Agua y saneamiento", ExpenseCategory.OTHER, 1, money("88.00")),
            new ExpenseConceptSeed("Papeleria recepcion", ExpenseCategory.OTHER, 2, money("37.00")),
            new ExpenseConceptSeed("Decoracion escaparate primavera", ExpenseCategory.OTHER, 1, money("96.00")),
            new ExpenseConceptSeed("Cafe y cortesia clientes", ExpenseCategory.OTHER, 3, money("42.00")),
            new ExpenseConceptSeed("Uniformes equipo", ExpenseCategory.OTHER, 5, money("125.00")),
            new ExpenseConceptSeed("Cuota asociacion barrio", ExpenseCategory.OTHER, 1, money("45.00")),
            new ExpenseConceptSeed("Tasa residuos", ExpenseCategory.OTHER, 1, money("68.00")),
            new ExpenseConceptSeed("Cristaleria office", ExpenseCategory.OTHER, 2, money("54.00")),
            new ExpenseConceptSeed("Imprevistos operativos", ExpenseCategory.OTHER, 1, money("73.00"))
    );
    private static final List<String> EXPENSE_NOTES = List.of(
            "Registrado para control mensual.",
            "Compra recurrente de operativa diaria.",
            "Facturado por proveedor habitual.",
            "Necesario para mantener el servicio activo.",
            "Ajuste de aprovisionamiento del mes."
    );
    private static final List<String> APPOINTMENT_NOTES = List.of(
            "Primera visita del cliente.",
            "Prefiere horario de manana.",
            "Solicita acabado natural.",
            "Llega con tiempo justo desde el trabajo.",
            "Revisar historial de color anterior.",
            "Recordar ofrecer mantenimiento en casa.",
            "Cliente habitual del equipo."
    );
    private static final List<String> SALE_NOTES = List.of(
            "Venta registrada en recepcion.",
            "Cliente aprovecha recomendacion del equipo.",
            "Cobro habitual al cierre de servicio.",
            "Operacion para reporte comercial.",
            "Venta de mostrador."
    );
    private static final List<String> CLIENT_FIRST_NAMES = List.of(
            "Lucia", "Carmen", "Paula", "Alba", "Nuria", "Elena", "Sara", "Noelia", "Irene", "Claudia",
            "Silvia", "Laura", "Marta", "Raquel", "Bea", "Sonia", "Natalia", "Monica", "Alicia", "Rocio",
            "Daniel", "Javier", "Diego", "Mario", "Pablo", "Adrian", "Hector", "Victor", "Ruben", "Marcos"
    );
    private static final List<String> CLIENT_LAST_NAMES = List.of(
            "Garcia", "Lopez", "Martinez", "Sanchez", "Perez", "Gomez", "Martin", "Jimenez", "Ruiz", "Hernandez",
            "Diaz", "Moreno", "Munoz", "Alonso", "Romero", "Navarro", "Torres", "Dominguez", "Vazquez", "Ramos",
            "Gil", "Serrano", "Blanco", "Molina", "Castro", "Ortega", "Delgado", "Suarez", "Benitez", "Prieto"
    );

    private final CategoryService categoryService;
    private final ServiceService serviceService;
    private final ClientService clientService;
    private final ProfessionalService professionalService;
    private final InventoryProductService inventoryProductService;
    private final ExpenseService expenseService;
    private final AppointmentService appointmentService;
    private final SaleService saleService;
    private final CategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final ClientRepository clientRepository;
    private final ProfessionalRepository professionalRepository;
    private final InventoryProductRepository inventoryProductRepository;
    private final ExpenseRepository expenseRepository;
    private final AppointmentRepository appointmentRepository;
    private final SaleRepository saleRepository;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Iniciando seed de datos demo de desarrollo");

        ExistingDataCounts existingDataCounts = snapshotCounts();
        if (existingDataCounts.hasAnyData()) {
            log.info(
                    "Seed de datos demo omitido porque la base ya contiene informacion. {}",
                    existingDataCounts.describe()
            );
            return;
        }

        SeedSummary summary = new SeedSummary();

        try {
            transactionTemplate.executeWithoutResult(status -> seedAll(summary));
        } catch (RuntimeException ex) {
            log.error("El seed de datos demo ha fallado y se revierte la transaccion", ex);
            throw ex;
        }

        log.info("Seed de datos demo completado. {}", summary.toLogMessage());
    }

    private void seedAll(SeedSummary summary) {
        Random random = new Random(RANDOM_SEED);

        Map<String, CategoryResponse> categoriesByName = seedCategories(summary);
        List<ServiceResponse> services = seedServices(categoriesByName, summary);
        List<ClientResponse> clients = seedClients(random, summary);
        List<ProfessionalResponse> professionals = seedProfessionals(summary);
        List<InventoryProductResponse> inventoryProducts = seedInventory(categoriesByName, summary);

        seedExpenses(inventoryProducts, random, summary);
        seedAppointments(clients, services, professionals, random, summary);
        seedManualSales(clients, services, random, summary);
    }

    private Map<String, CategoryResponse> seedCategories(SeedSummary summary) {
        Map<String, CategoryResponse> categoriesByName = new LinkedHashMap<>();

        for (CategorySeed categorySeed : CATEGORY_SEEDS) {
            CategoryRequest request = new CategoryRequest();
            request.setName(categorySeed.name());
            request.setColor(categorySeed.color());
            request.setIcon(categorySeed.icon());

            CategoryResponse response = categoryService.create(request);
            categoriesByName.put(categorySeed.name(), response);
            summary.categoriesCreated++;
        }

        return categoriesByName;
    }

    private List<ServiceResponse> seedServices(Map<String, CategoryResponse> categoriesByName, SeedSummary summary) {
        List<ServiceResponse> services = new ArrayList<>();

        for (ServiceSeed serviceSeed : SERVICE_SEEDS) {
            ServiceRequest request = new ServiceRequest();
            request.setName(serviceSeed.name());
            request.setDescription(serviceSeed.description());
            request.setDurationMinutes(serviceSeed.durationMinutes());
            request.setPrice(serviceSeed.price());
            request.setActive(serviceSeed.active());
            request.setCategoryId(categoriesByName.get(serviceSeed.categoryName()).getId());

            services.add(serviceService.create(request));
            summary.servicesCreated++;
        }

        return services;
    }

    private List<ClientResponse> seedClients(Random random, SeedSummary summary) {
        List<ClientResponse> clients = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();

        clients.add(createClient(WALK_IN_CLIENT_NAME, buildPhone(0), "caja@demo-pymes.es"));
        usedNames.add(WALK_IN_CLIENT_NAME.toLowerCase(Locale.ROOT));
        summary.clientsCreated++;

        int sequence = 1;
        while (clients.size() < TARGET_CLIENT_COUNT) {
            String fullName = buildClientFullName(random);
            if (!usedNames.add(fullName.toLowerCase(Locale.ROOT))) {
                continue;
            }

            String email = sequence % 3 == 0 ? null : buildClientEmail(fullName, sequence);
            clients.add(createClient(fullName, buildPhone(sequence), email));
            summary.clientsCreated++;
            sequence++;
        }

        return clients;
    }

    private List<ProfessionalResponse> seedProfessionals(SeedSummary summary) {
        List<ProfessionalResponse> professionals = new ArrayList<>();

        for (ProfessionalSeed professionalSeed : PROFESSIONAL_SEEDS) {
            ProfessionalRequest request = new ProfessionalRequest(
                    professionalSeed.fullName(),
                    professionalSeed.specialty(),
                    professionalSeed.phone(),
                    professionalSeed.email(),
                    PROFESSIONAL_DEMO_PASSWORD,
                    professionalSeed.active()
            );

            professionals.add(professionalService.create(request));
            summary.professionalsCreated++;
        }

        return professionals;
    }

    private List<InventoryProductResponse> seedInventory(Map<String, CategoryResponse> categoriesByName, SeedSummary summary) {
        List<InventoryProductResponse> inventoryProducts = new ArrayList<>();

        for (int index = 0; index < INVENTORY_TEMPLATES.size(); index++) {
            InventoryTemplate template = INVENTORY_TEMPLATES.get(index);
            CategoryResponse category = categoriesByName.get(template.categoryName());

            CreateInventoryProductRequest request = buildInventoryRequest(template, category.getId(), index);
            InventoryProductResponse response = inventoryProductService.createProduct(request);

            inventoryProducts.add(response);
            summary.inventoryProductsCreated++;

            if (request.getStock() > 0) {
                summary.automaticInventoryExpensesCreated++;
            }
        }

        return inventoryProducts;
    }

    private void seedExpenses(List<InventoryProductResponse> inventoryProducts, Random random, SeedSummary summary) {
        LocalDate today = LocalDate.now();
        List<InventoryProductResponse> shuffledProducts = new ArrayList<>(inventoryProducts);
        Collections.shuffle(shuffledProducts, random);

        for (int index = 0; index < TARGET_SUPPLIES_EXPENSES; index++) {
            InventoryProductResponse product = shuffledProducts.get(index % shuffledProducts.size());
            int quantity = 1 + (index % 6);

            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .expenseDate(historicalDate(today, random, index, 330))
                    .concept(product.getName())
                    .category(ExpenseCategory.SUPPLIES)
                    .quantity(quantity)
                    .amount(calculateExpenseAmount(product.getPurchasePrice(), quantity))
                    .notes(randomItem(EXPENSE_NOTES, random))
                    .build();

            expenseService.createExpense(request);
            summary.manualExpensesCreated++;
        }

        for (int index = 0; index < SERVICES_EXPENSE_SEEDS.size(); index++) {
            ExpenseConceptSeed conceptSeed = SERVICES_EXPENSE_SEEDS.get(index);

            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .expenseDate(historicalDate(today, random, TARGET_SUPPLIES_EXPENSES + index, 330))
                    .concept(conceptSeed.concept())
                    .category(conceptSeed.category())
                    .quantity(conceptSeed.quantity())
                    .amount(conceptSeed.amount())
                    .notes(randomItem(EXPENSE_NOTES, random))
                    .build();

            expenseService.createExpense(request);
            summary.manualExpensesCreated++;
        }

        for (int index = 0; index < OTHER_EXPENSE_SEEDS.size(); index++) {
            ExpenseConceptSeed conceptSeed = OTHER_EXPENSE_SEEDS.get(index);

            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .expenseDate(historicalDate(today, random, TARGET_SUPPLIES_EXPENSES + SERVICES_EXPENSE_SEEDS.size() + index, 330))
                    .concept(conceptSeed.concept())
                    .category(conceptSeed.category())
                    .quantity(conceptSeed.quantity())
                    .amount(conceptSeed.amount())
                    .notes(randomItem(EXPENSE_NOTES, random))
                    .build();

            expenseService.createExpense(request);
            summary.manualExpensesCreated++;
        }
    }

    private void seedAppointments(
            List<ClientResponse> clients,
            List<ServiceResponse> services,
            List<ProfessionalResponse> professionals,
            Random random,
            SeedSummary summary
    ) {
        LocalDate today = LocalDate.now();
        List<ClientResponse> businessClients = clients.stream()
                .filter(client -> !WALK_IN_CLIENT_NAME.equalsIgnoreCase(client.getFullName()))
                .toList();
        List<ServiceResponse> activeServices = services.stream()
                .filter(service -> Boolean.TRUE.equals(service.getActive()))
                .toList();
        List<ProfessionalResponse> activeProfessionals = professionals.stream()
                .filter(professional -> Boolean.TRUE.equals(professional.getActive()))
                .toList();

        for (int index = 0; index < TARGET_APPOINTMENT_COUNT; index++) {
            ClientResponse client = randomItem(businessClients, random);
            ServiceResponse service = randomItem(activeServices, random);
            ProfessionalResponse professional = activeProfessionals.get(index % activeProfessionals.size());
            LocalDate appointmentDate = pickAppointmentDate(today, random, index);
            LocalTime appointmentTime = randomItem(APPOINTMENT_TIME_SLOTS, random);
            AppointmentStatus finalStatus = determineAppointmentStatus(today, appointmentDate, random);

            AppointmentRequest request = new AppointmentRequest();
            request.setClientId(client.getId());
            request.setServiceId(service.getId());
            request.setPrice(adjustAppointmentPrice(service.getPrice(), index));
            request.setProfessionalId(professional.getId());
            request.setProfessionalName(null);
            request.setAppointmentDate(appointmentDate);
            request.setAppointmentTime(appointmentTime);
            request.setStatus(finalStatus == AppointmentStatus.CONFIRMED ? AppointmentStatus.PENDING : finalStatus);
            request.setInternalNotes(randomItem(APPOINTMENT_NOTES, random));

            AppointmentResponse createdAppointment = appointmentService.createAppointment(request);
            summary.appointmentsCreated++;

            if (finalStatus == AppointmentStatus.CONFIRMED) {
                request.setStatus(AppointmentStatus.CONFIRMED);
                appointmentService.updateAppointment(createdAppointment.getId(), request);
                summary.automaticAppointmentSalesCreated++;
            }
        }
    }

    private void seedManualSales(
            List<ClientResponse> clients,
            List<ServiceResponse> services,
            Random random,
            SeedSummary summary
    ) {
        LocalDate today = LocalDate.now();
        List<ClientResponse> billableClients = clients.stream()
                .filter(client -> !WALK_IN_CLIENT_NAME.equalsIgnoreCase(client.getFullName()))
                .toList();
        List<ServiceResponse> activeServices = services.stream()
                .filter(service -> Boolean.TRUE.equals(service.getActive()))
                .toList();

        List<InventoryProductEntity> activeProducts = inventoryProductRepository.findAll().stream()
                .filter(product -> Boolean.TRUE.equals(product.getActive()))
                .toList();

        Map<Long, Integer> remainingStockByProductId = activeProducts.stream()
                .collect(Collectors.toMap(InventoryProductEntity::getId, InventoryProductEntity::getStock));

        for (int index = 0; index < TARGET_MANUAL_SALES; index++) {
            boolean productSaleRequested = index % 10 >= 6;

            if (productSaleRequested) {
                List<InventoryProductEntity> sellableProducts = activeProducts.stream()
                        .filter(product -> remainingStockByProductId.getOrDefault(product.getId(), 0) > 0)
                        .toList();

                if (!sellableProducts.isEmpty()) {
                    InventoryProductEntity product = randomItem(sellableProducts, random);
                    int availableStock = remainingStockByProductId.get(product.getId());
                    int quantity = 1 + random.nextInt(Math.min(3, availableStock));

                    saleService.createManualSale(
                            CreateSaleRequest.builder()
                                    .clientId(resolveManualSaleClientId(billableClients, random, index))
                                    .itemType(SaleItemType.PRODUCT)
                                    .inventoryProductId(product.getId())
                                    .unitPrice(calculateProductSalePrice(product))
                                    .quantity(quantity)
                                    .paymentMethod(randomItem(PAYMENT_METHODS, random))
                                    .saleDate(historicalDate(today, random, 500 + index, 330))
                                    .saleTime(randomItem(SALE_TIME_SLOTS, random))
                                    .notes(randomItem(SALE_NOTES, random))
                                    .build()
                    );

                    remainingStockByProductId.put(product.getId(), availableStock - quantity);
                    summary.manualSalesCreated++;
                    continue;
                }
            }

            ServiceResponse service = randomItem(activeServices, random);

            saleService.createManualSale(
                    CreateSaleRequest.builder()
                            .clientId(resolveManualSaleClientId(billableClients, random, index))
                            .itemType(SaleItemType.SERVICE)
                            .serviceId(service.getId())
                            .unitPrice(calculateServiceSalePrice(service.getPrice(), index))
                            .quantity(1)
                            .paymentMethod(randomItem(PAYMENT_METHODS, random))
                            .saleDate(historicalDate(today, random, 500 + index, 330))
                            .saleTime(randomItem(SALE_TIME_SLOTS, random))
                            .notes(randomItem(SALE_NOTES, random))
                            .build()
            );

            summary.manualSalesCreated++;
        }
    }

    private ClientResponse createClient(String fullName, String phone, String email) {
        ClientRequest request = new ClientRequest();
        request.setFullName(fullName);
        request.setPhone(phone);
        request.setEmail(email);
        return clientService.createClient(request);
    }

    private CreateInventoryProductRequest buildInventoryRequest(InventoryTemplate template, Long categoryId, int index) {
        int minimumStock = 2 + (index % 5);
        int stock = switch (index % 6) {
            case 0 -> 0;
            case 1 -> Math.max(1, minimumStock - 1);
            case 2 -> minimumStock;
            default -> minimumStock + 4 + (index % 8);
        };

        return CreateInventoryProductRequest.builder()
                .name(template.name())
                .description(buildInventoryDescription(template.categoryName(), template.name()))
                .categoryId(categoryId)
                .stock(stock)
                .minimumStock(minimumStock)
                .sku(index % 4 == 0 ? null : buildSku(template.categoryName(), index))
                .purchasePrice(template.purchasePrice())
                .build();
    }

    private String buildInventoryDescription(String categoryName, String productName) {
        return switch (categoryName) {
            case "Retail" -> "Producto de venta al publico para mantenimiento en casa: " + productName + ".";
            case "Coloracion" -> "Consumible tecnico de coloracion para uso profesional: " + productName + ".";
            case "Barberia" -> "Producto de barberia y acabado para servicio en cabina: " + productName + ".";
            case "Estetica" -> "Cosmetico de apoyo para tratamientos faciales: " + productName + ".";
            case "Unas" -> "Producto tecnico de manicura y pedicura: " + productName + ".";
            default -> "Producto profesional de inventario: " + productName + ".";
        };
    }

    private String buildSku(String categoryName, int index) {
        String normalizedCategory = normalizeToken(categoryName);
        String categoryPrefix = normalizedCategory.substring(0, Math.min(3, normalizedCategory.length())).toUpperCase(Locale.ROOT);
        return "DEM-" + categoryPrefix + "-" + String.format("%03d", index + 1);
    }

    private String buildClientFullName(Random random) {
        String firstName = randomItem(CLIENT_FIRST_NAMES, random);
        String lastNameOne = randomItem(CLIENT_LAST_NAMES, random);
        String lastNameTwo = randomItem(CLIENT_LAST_NAMES, random);
        return firstName + " " + lastNameOne + " " + lastNameTwo;
    }

    private String buildPhone(int sequence) {
        int prefix = sequence % 2 == 0 ? 6 : 7;
        int suffix = 10_000_000 + ((sequence * 431) % 90_000_000);
        return prefix + String.format("%08d", suffix);
    }

    private String buildClientEmail(String fullName, int sequence) {
        return normalizeToken(fullName) + "." + String.format("%03d", sequence) + "@demo-pymes.es";
    }

    private AppointmentStatus determineAppointmentStatus(LocalDate today, LocalDate appointmentDate, Random random) {
        int roll = random.nextInt(100);

        if (appointmentDate.isBefore(today)) {
            if (roll < 72) {
                return AppointmentStatus.CONFIRMED;
            }
            if (roll < 88) {
                return AppointmentStatus.CANCELLED;
            }
            return AppointmentStatus.PENDING;
        }

        if (appointmentDate.isEqual(today)) {
            if (roll < 45) {
                return AppointmentStatus.CONFIRMED;
            }
            if (roll < 85) {
                return AppointmentStatus.PENDING;
            }
            return AppointmentStatus.CANCELLED;
        }

        if (roll < 18) {
            return AppointmentStatus.CONFIRMED;
        }
        if (roll < 82) {
            return AppointmentStatus.PENDING;
        }
        return AppointmentStatus.CANCELLED;
    }

    private LocalDate pickAppointmentDate(LocalDate today, Random random, int index) {
        if (index < 90) {
            return today.minusDays(1L + random.nextInt(120));
        }
        if (index < 108) {
            return today;
        }
        return today.plusDays(1L + random.nextInt(60));
    }

    private LocalDate historicalDate(LocalDate today, Random random, int index, int maxPastDays) {
        int daysBack = Math.min(maxPastDays, (index * 17 + random.nextInt(31)) % (maxPastDays + 1));
        return today.minusDays(daysBack);
    }

    private BigDecimal calculateExpenseAmount(BigDecimal purchasePrice, int quantity) {
        return purchasePrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private double adjustAppointmentPrice(Double basePrice, int index) {
        double factor = index % 9 == 0 ? 0.92 : (index % 13 == 0 ? 1.08 : 1.00);
        return BigDecimal.valueOf(basePrice)
                .multiply(BigDecimal.valueOf(factor))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private BigDecimal calculateServiceSalePrice(Double basePrice, int index) {
        BigDecimal factor = index % 7 == 0 ? new BigDecimal("0.95") : BigDecimal.ONE;
        return BigDecimal.valueOf(basePrice)
                .multiply(factor)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateProductSalePrice(InventoryProductEntity product) {
        BigDecimal purchasePrice = product.getPurchasePrice() != null ? product.getPurchasePrice() : money("6.00");
        return purchasePrice.multiply(new BigDecimal("1.85")).setScale(2, RoundingMode.HALF_UP);
    }

    private Long resolveManualSaleClientId(List<ClientResponse> billableClients, Random random, int index) {
        if (index % 5 == 0) {
            return null;
        }
        return randomItem(billableClients, random).getId();
    }

    private ExistingDataCounts snapshotCounts() {
        return new ExistingDataCounts(
                categoryRepository.count(),
                serviceRepository.count(),
                clientRepository.count(),
                professionalRepository.count(),
                inventoryProductRepository.count(),
                expenseRepository.count(),
                appointmentRepository.count(),
                saleRepository.count()
        );
    }

    private static <T> T randomItem(List<T> items, Random random) {
        return items.get(random.nextInt(items.size()));
    }

    private static List<LocalTime> buildTimeSlots(LocalTime start, LocalTime end, int minutesStep) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime current = start;

        while (!current.isAfter(end)) {
            slots.add(current);
            current = current.plusMinutes(minutesStep);
        }

        return Collections.unmodifiableList(slots);
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeToken(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace(" ", "-")
                .replace(".", "")
                .replace(",", "");
    }

    private record CategorySeed(String name, String color, String icon) {
    }

    private record ServiceSeed(
            String name,
            String description,
            int durationMinutes,
            double price,
            boolean active,
            String categoryName
    ) {
    }

    private record ProfessionalSeed(String fullName, String specialty, String phone, String email, boolean active) {
    }

    private record InventoryTemplate(String name, String categoryName, BigDecimal purchasePrice) {
    }

    private record ExpenseConceptSeed(String concept, ExpenseCategory category, int quantity, BigDecimal amount) {
    }

    private record ExistingDataCounts(
            long categories,
            long services,
            long clients,
            long professionals,
            long inventoryProducts,
            long expenses,
            long appointments,
            long sales
    ) {
        boolean hasAnyData() {
            return categories > 0
                    || services > 0
                    || clients > 0
                    || professionals > 0
                    || inventoryProducts > 0
                    || expenses > 0
                    || appointments > 0
                    || sales > 0;
        }

        String describe() {
            return "categorias=" + categories
                    + ", servicios=" + services
                    + ", clientes=" + clients
                    + ", profesionales=" + professionals
                    + ", inventario=" + inventoryProducts
                    + ", gastos=" + expenses
                    + ", citas=" + appointments
                    + ", ventas=" + sales;
        }
    }

    private static final class SeedSummary {
        private int categoriesCreated;
        private int servicesCreated;
        private int clientsCreated;
        private int professionalsCreated;
        private int inventoryProductsCreated;
        private int automaticInventoryExpensesCreated;
        private int manualExpensesCreated;
        private int appointmentsCreated;
        private int automaticAppointmentSalesCreated;
        private int manualSalesCreated;

        private String toLogMessage() {
            return "categorias=" + categoriesCreated
                    + ", servicios=" + servicesCreated
                    + ", clientes=" + clientsCreated
                    + ", profesionales=" + professionalsCreated
                    + ", inventario=" + inventoryProductsCreated
                    + ", gastosManuales=" + manualExpensesCreated
                    + ", gastosAutomaticosInventario=" + automaticInventoryExpensesCreated
                    + ", citas=" + appointmentsCreated
                    + ", ventasManuales=" + manualSalesCreated
                    + ", ventasAutomaticasPorCita=" + automaticAppointmentSalesCreated;
        }
    }
}
