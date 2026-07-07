package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.AppointmentStatus;
import es.pedro.pymes_backend.domain.ExpenseCategory;
import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.domain.InventoryProductVariantEntity;
import es.pedro.pymes_backend.domain.PaymentMethod;
import es.pedro.pymes_backend.domain.SaleItemType;
import es.pedro.pymes_backend.infrastructure.repository.AppointmentRepository;
import es.pedro.pymes_backend.infrastructure.repository.CategoryRepository;
import es.pedro.pymes_backend.infrastructure.repository.ClientRepository;
import es.pedro.pymes_backend.infrastructure.repository.ExpenseRepository;
import es.pedro.pymes_backend.infrastructure.repository.InventoryProductRepository;
import es.pedro.pymes_backend.infrastructure.repository.InventoryProductVariantRepository;
import es.pedro.pymes_backend.infrastructure.repository.ProfessionalRepository;
import es.pedro.pymes_backend.infrastructure.repository.ReportQueryRepository;
import es.pedro.pymes_backend.infrastructure.repository.SaleRepository;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import es.pedro.pymes_backend.infrastructure.repository.report.ReportPeriodTotals;
import es.pedro.pymes_backend.infrastructure.web.dto.AppointmentRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.AppointmentResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.CategoryRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.CategoryResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.ClientRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.ClientResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateExpenseRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateInventoryProductRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateInventoryProductVariantRequest;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "demo-data", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private static final LocalDate SEED_START_DATE = LocalDate.of(2024, 1, 1);
    private static final LocalDate SEED_END_DATE = LocalDate.of(2027, 12, 31);
    private static final YearMonth FIRST_SEED_MONTH = YearMonth.from(SEED_START_DATE);
    private static final YearMonth LAST_SEED_MONTH = YearMonth.from(SEED_END_DATE);
    private static final String WALK_IN_CLIENT_NAME = "Al contado";
    private static final String PROFESSIONAL_DEMO_PASSWORD = "1234";
    private static final int TARGET_TOTAL_CLIENTS = 360;
    private static final int WEEKLY_PRODUCT_SALES_MIN = 5;
    private static final int TARGET_INVENTORY_PRODUCTS = 160;
    private static final List<PaymentMethod> PAYMENT_METHODS = Arrays.asList(PaymentMethod.values());
    private static final List<LocalTime> APPOINTMENT_TIME_SLOTS = buildTimeSlots(LocalTime.of(9, 0), LocalTime.of(19, 30), 30);

    private static final List<CategorySeed> CATEGORY_SEEDS = buildCategorySeeds();
    private static final List<ServiceSeed> SERVICE_SEEDS = buildServiceSeeds();
    private static final List<ProfessionalSeed> PROFESSIONAL_SEEDS = buildProfessionalSeeds();
    private static final List<RecurringExpenseTemplate> RECURRING_EXPENSE_TEMPLATES = buildRecurringExpenseTemplates();
    private static final List<StrategicExpensePlan> STRATEGIC_EXPENSE_PLANS = buildStrategicExpensePlans();
    private static final List<String> CLIENT_FIRST_NAMES = buildClientFirstNames();
    private static final List<String> CLIENT_LAST_NAMES = buildClientLastNames();
    private static final List<String> APPOINTMENT_NOTES = buildAppointmentNotes();
    private static final List<String> EXPENSE_NOTES = buildExpenseNotes();
    private static final List<String> SALE_NOTES = buildSaleNotes();
    private static final Map<YearMonth, MonthPlan> MONTH_PLANS = buildMonthPlans();
    private static final List<InventoryTemplate> INVENTORY_TEMPLATES = buildInventoryTemplates();

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
    private final InventoryProductVariantRepository inventoryProductVariantRepository;
    private final ExpenseRepository expenseRepository;
    private final AppointmentRepository appointmentRepository;
    private final SaleRepository saleRepository;
    private final ReportQueryRepository reportQueryRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Iniciando seed de datos demo de desarrollo ampliado");

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
            seedAll(summary);
        } catch (RuntimeException ex) {
            log.error("El seed de datos demo ampliado ha fallado", ex);
            throw ex;
        }

        log.info("Seed de datos demo ampliado completado. {}", summary.toLogMessage());
    }

    private void seedAll(SeedSummary summary) {
        Map<String, CategoryResponse> categoriesByName = seedCategories(summary);
        List<ServiceResponse> services = seedServices(categoriesByName, summary);
        List<ClientResponse> clients = seedClients(summary);
        List<SeededProfessional> professionals = seedProfessionals(summary);
        List<SeededInventoryProduct> inventoryProducts = seedInventory(categoriesByName, summary);

        seedExpenses(inventoryProducts, summary);
        seedAppointments(clients, services, professionals, summary);
        seedManualSales(clients, services, inventoryProducts, summary);
        shapeEconomicScenarios(services, summary);

        ExistingDataCounts finalCounts = snapshotCounts();
        summary.totalCategories = finalCounts.categories();
        summary.totalServices = finalCounts.services();
        summary.totalClients = finalCounts.clients();
        summary.totalProfessionals = finalCounts.professionals();
        summary.totalInventoryProducts = finalCounts.inventoryProducts();
        summary.totalExpenses = finalCounts.expenses();
        summary.totalAppointments = finalCounts.appointments();
        summary.totalSales = finalCounts.sales();
    }

    private Map<String, CategoryResponse> seedCategories(SeedSummary summary) {
        Map<String, CategoryResponse> categoriesByName = new LinkedHashMap<>();

        for (CategorySeed categorySeed : CATEGORY_SEEDS) {
            CategoryRequest request = new CategoryRequest();
            request.setName(categorySeed.name());
            request.setColor(categorySeed.color());
            request.setIcon(categorySeed.icon());

            CategoryResponse created = categoryService.create(request);
            categoriesByName.put(categorySeed.name(), created);
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

    private List<ClientResponse> seedClients(SeedSummary summary) {
        List<ClientResponse> clients = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();

        clients.add(createClient(WALK_IN_CLIENT_NAME, "", null));
        usedNames.add(WALK_IN_CLIENT_NAME.toLowerCase(Locale.ROOT));
        summary.clientsCreated++;

        int sequence = 0;
        while (clients.size() < TARGET_TOTAL_CLIENTS) {
            String fullName = buildClientFullName(sequence);
            if (!usedNames.add(fullName.toLowerCase(Locale.ROOT))) {
                sequence++;
                continue;
            }

            String email = sequence % 4 == 0 ? null : buildClientEmail(fullName, sequence);
            clients.add(createClient(fullName, buildPhone(sequence), email));
            summary.clientsCreated++;
            sequence++;
        }

        return clients;
    }

    private List<SeededProfessional> seedProfessionals(SeedSummary summary) {
        List<SeededProfessional> professionals = new ArrayList<>();

        for (ProfessionalSeed professionalSeed : PROFESSIONAL_SEEDS) {
            ProfessionalRequest request = new ProfessionalRequest(
                    professionalSeed.fullName(),
                    professionalSeed.specialty(),
                    professionalSeed.phone(),
                    professionalSeed.email(),
                    PROFESSIONAL_DEMO_PASSWORD,
                    professionalSeed.active()
            );

            ProfessionalResponse created = professionalService.create(request);
            professionals.add(new SeededProfessional(created, professionalSeed));
            summary.professionalsCreated++;
        }

        return professionals;
    }

    private List<SeededInventoryProduct> seedInventory(
            Map<String, CategoryResponse> categoriesByName,
            SeedSummary summary
    ) {
        List<SeededInventoryProduct> products = new ArrayList<>();

        for (int index = 0; index < INVENTORY_TEMPLATES.size(); index++) {
            InventoryTemplate template = INVENTORY_TEMPLATES.get(index);
            if (template.hasVariants()) {
                CreateInventoryProductRequest request = CreateInventoryProductRequest.builder()
                        .name(template.name())
                        .description(buildInventoryDescription(template.categoryName(), template.name(), template.sellable()))
                        .categoryId(categoriesByName.get(template.categoryName()).getId())
                        .hasVariants(true)
                        .variants(buildVariantRequests(template))
                        .build();

                InventoryProductResponse created = inventoryProductService.createProduct(request);
                List<SeededInventoryVariant> variants = buildSeededVariants(created, template);
                products.add(new SeededInventoryProduct(
                        created.getId(),
                        created.getName(),
                        null,
                        template.sellable(),
                        resolveStockProfile(created.getStock(), created.getMinimumStock()),
                        template.categoryName(),
                        true,
                        variants
                ));

                summary.inventoryProductsCreated++;
                summary.inventoryVariantsCreated += variants.size();
                summary.automaticInventoryExpensesCreated += variants.stream()
                        .mapToInt(variant -> variant.initialStock() > 0 ? 1 : 0)
                        .sum();
                continue;
            }

            StockProfile stockProfile = template.stock() != null
                    ? resolveStockProfile(template.stock(), template.minimumStock())
                    : resolveStockProfile(index);
            int minimumStock = template.minimumStock() != null ? template.minimumStock() : 2 + (index % 5);
            int stock = template.stock() != null ? template.stock() : resolveInitialStock(stockProfile, minimumStock, index);

            CreateInventoryProductRequest request = CreateInventoryProductRequest.builder()
                    .name(template.name())
                    .description(buildInventoryDescription(template.categoryName(), template.name(), template.sellable()))
                    .categoryId(categoriesByName.get(template.categoryName()).getId())
                    .stock(stock)
                    .minimumStock(minimumStock)
                    .sku(template.sku() != null ? template.sku() : (index % 5 == 0 ? null : buildSku(template.categoryName(), index)))
                    .purchasePrice(template.purchasePrice())
                    .hasVariants(false)
                    .build();

            InventoryProductResponse created = inventoryProductService.createProduct(request);
            products.add(new SeededInventoryProduct(
                    created.getId(),
                    created.getName(),
                    created.getPurchasePrice(),
                    template.sellable(),
                    stockProfile,
                    template.categoryName(),
                    false,
                    List.of()
            ));

            summary.inventoryProductsCreated++;
            if (stock > 0) {
                summary.automaticInventoryExpensesCreated++;
            }
        }

        return products;
    }

    private List<CreateInventoryProductVariantRequest> buildVariantRequests(InventoryTemplate template) {
        return template.variants().stream()
                .map(variant -> CreateInventoryProductVariantRequest.builder()
                        .variantLabel(variant.variantLabel())
                        .sku(variant.sku())
                        .stock(variant.stock())
                        .minimumStock(variant.minimumStock())
                        .purchasePrice(variant.purchasePrice())
                        .attributes(variant.attributes())
                        .build())
                .toList();
    }

    private List<SeededInventoryVariant> buildSeededVariants(
            InventoryProductResponse product,
            InventoryTemplate template
    ) {
        Map<String, InventoryVariantTemplate> templatesByLabel = template.variants().stream()
                .collect(Collectors.toMap(InventoryVariantTemplate::variantLabel, variant -> variant));

        return product.getVariants().stream()
                .map(variant -> {
                    InventoryVariantTemplate variantTemplate = templatesByLabel.get(variant.getVariantLabel());
                    int initialStock = variantTemplate != null ? variantTemplate.stock() : variant.getStock();
                    return new SeededInventoryVariant(
                            variant.getId(),
                            variant.getVariantLabel(),
                            variant.getSku(),
                            variant.getPurchasePrice(),
                            initialStock,
                            resolveStockProfile(variant.getStock(), variant.getMinimumStock())
                    );
                })
                .toList();
    }

    private List<SeededInventoryItem> buildInventoryItems(List<SeededInventoryProduct> inventoryProducts) {
        List<SeededInventoryItem> items = new ArrayList<>();

        for (SeededInventoryProduct product : inventoryProducts) {
            if (product.hasVariants()) {
                for (SeededInventoryVariant variant : product.variants()) {
                    items.add(new SeededInventoryItem(
                            product.id(),
                            variant.id(),
                            product.name(),
                            product.name() + " - " + variant.variantLabel(),
                            variant.purchasePrice(),
                            product.sellable(),
                            variant.stockProfile(),
                            product.categoryName()
                    ));
                }
                continue;
            }

            items.add(new SeededInventoryItem(
                    product.id(),
                    null,
                    product.name(),
                    product.name(),
                    product.purchasePrice(),
                    product.sellable(),
                    product.stockProfile(),
                    product.categoryName()
            ));
        }

        return items;
    }

    private SeededInventoryItem requireDemoInventoryItem(List<SeededInventoryItem> inventoryItems, String itemName) {
        return inventoryItems.stream()
                .filter(item -> item.itemName().equals(itemName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe item demo de inventario: " + itemName));
    }

    private void seedExpenses(List<SeededInventoryProduct> inventoryProducts, SeedSummary summary) {
        seedRecurringExpenses(summary);
        seedShowcaseSupplyExpenses(inventoryProducts, summary);
        seedMonthlySupplyExpenses(inventoryProducts, summary);
        seedStrategicExpenses(summary);
    }

    private void seedRecurringExpenses(SeedSummary summary) {
        YearMonth month = FIRST_SEED_MONTH;

        while (!month.isAfter(LAST_SEED_MONTH)) {
            MonthPlan plan = MONTH_PLANS.get(month);

            for (RecurringExpenseTemplate template : RECURRING_EXPENSE_TEMPLATES) {
                expenseService.createExpense(
                        CreateExpenseRequest.builder()
                                .expenseDate(resolveExpenseDate(month, template.dayOfMonth()))
                                .concept(template.concept())
                                .category(template.category())
                                .quantity(template.quantity())
                                .amount(calculateRecurringExpenseAmount(template.baseAmount(), plan, month))
                                .notes(noteFrom(EXPENSE_NOTES, month.getMonthValue() + template.dayOfMonth()))
                                .build()
                );
                summary.manualExpensesCreated++;
            }

            if (month.getMonthValue() == 1) {
                expenseService.createExpense(
                        CreateExpenseRequest.builder()
                                .expenseDate(resolveExpenseDate(month, 20))
                                .concept("Seguro multirriesgo anual")
                                .category(ExpenseCategory.OTHER)
                                .quantity(1)
                                .amount(money(month.getYear() >= 2027 ? "840.00" : "760.00"))
                                .notes("Renovacion anual del seguro del negocio.")
                                .build()
                );
                summary.manualExpensesCreated++;
            }

            month = month.plusMonths(1);
        }
    }

    private void seedShowcaseSupplyExpenses(List<SeededInventoryProduct> inventoryProducts, SeedSummary summary) {
        List<SeededInventoryItem> inventoryItems = buildInventoryItems(inventoryProducts);
        List<SeededInventoryItem> showcaseItems = List.of(
                requireDemoInventoryItem(inventoryItems, "Champu L'Oreal Prof"),
                requireDemoInventoryItem(inventoryItems, "Acondicionador Kerastase"),
                requireDemoInventoryItem(inventoryItems, "Camiseta Basica - Roja / M"),
                requireDemoInventoryItem(inventoryItems, "Tornillo Galvanizado - 6mm / 40mm")
        );

        for (int index = 0; index < showcaseItems.size(); index++) {
            SeededInventoryItem item = showcaseItems.get(index);
            int quantity = item.variant() && item.itemName().startsWith("Tornillo") ? 20 : 4 + index;

            expenseService.createExpense(
                    CreateExpenseRequest.builder()
                            .expenseDate(LocalDate.of(2026, 3, 4).plusDays(index * 7L))
                            .concept(item.itemName())
                            .category(ExpenseCategory.SUPPLIES)
                            .quantity(quantity)
                            .amount(item.purchasePrice())
                            .notes("Reposicion demo con referencia directa de inventario.")
                            .inventoryProductId(item.productId())
                            .inventoryProductVariantId(item.variantId())
                            .build()
            );

            summary.manualExpensesCreated++;
        }
    }

    private void seedMonthlySupplyExpenses(List<SeededInventoryProduct> inventoryProducts, SeedSummary summary) {
        List<SeededInventoryItem> supplyCandidates = buildInventoryItems(inventoryProducts).stream()
                .filter(SeededInventoryItem::sellable)
                .filter(item -> item.stockProfile() == StockProfile.HIGH || item.stockProfile() == StockProfile.BALANCED)
                .toList();

        YearMonth month = FIRST_SEED_MONTH;
        int globalIndex = 0;

        while (!month.isAfter(LAST_SEED_MONTH)) {
            MonthPlan plan = MONTH_PLANS.get(month);
            int supplyCount = plan.targetOutcome() == OutcomeTarget.GAIN ? 3 : 2;

            for (int index = 0; index < supplyCount; index++) {
                SeededInventoryItem item = supplyCandidates.get(Math.floorMod(globalIndex * 3 + index, supplyCandidates.size()));
                int quantity = 3 + Math.floorMod(globalIndex + index, 5);

                expenseService.createExpense(
                        CreateExpenseRequest.builder()
                                .expenseDate(resolveExpenseDate(month, 8 + (index * 7)))
                                .concept(item.itemName())
                                .category(ExpenseCategory.SUPPLIES)
                                .quantity(quantity)
                                .amount(item.purchasePrice())
                                .notes(noteFrom(EXPENSE_NOTES, globalIndex + index))
                                .inventoryProductId(item.productId())
                                .inventoryProductVariantId(item.variantId())
                                .build()
                );

                summary.manualExpensesCreated++;
                globalIndex++;
            }

            month = month.plusMonths(1);
        }
    }

    private void seedStrategicExpenses(SeedSummary summary) {
        for (StrategicExpensePlan plan : STRATEGIC_EXPENSE_PLANS) {
            expenseService.createExpense(
                    CreateExpenseRequest.builder()
                            .expenseDate(resolveExpenseDate(plan.month(), plan.dayOfMonth()))
                            .concept(plan.concept())
                            .category(plan.category())
                            .quantity(plan.quantity())
                            .amount(plan.amount())
                            .notes(plan.notes())
                            .build()
            );
            summary.manualExpensesCreated++;
        }
    }

    private void seedAppointments(
            List<ClientResponse> clients,
            List<ServiceResponse> services,
            List<SeededProfessional> professionals,
            SeedSummary summary
    ) {
        ensureDailyAppointmentsCoverage(clients, services, professionals, summary);
        seedAdditionalAppointments(clients, services, professionals, summary);
    }

    private void ensureDailyAppointmentsCoverage(
            List<ClientResponse> clients,
            List<ServiceResponse> services,
            List<SeededProfessional> professionals,
            SeedSummary summary
    ) {
        Map<String, List<ServiceResponse>> servicesByCategory = services.stream()
                .filter(service -> Boolean.TRUE.equals(service.getActive()))
                .collect(Collectors.groupingBy(service -> service.getCategory().getName()));

        LocalDate currentDate = SEED_START_DATE;
        int sequence = 0;

        while (!currentDate.isAfter(SEED_END_DATE)) {
            createAppointmentForDate(currentDate, sequence, true, clients, servicesByCategory, professionals, summary);
            summary.daysCoveredWithAppointments++;
            currentDate = currentDate.plusDays(1);
            sequence++;
        }
    }

    private void seedAdditionalAppointments(
            List<ClientResponse> clients,
            List<ServiceResponse> services,
            List<SeededProfessional> professionals,
            SeedSummary summary
    ) {
        Map<String, List<ServiceResponse>> servicesByCategory = services.stream()
                .filter(service -> Boolean.TRUE.equals(service.getActive()))
                .collect(Collectors.groupingBy(service -> service.getCategory().getName()));

        YearMonth month = FIRST_SEED_MONTH;
        int sequence = 10_000;

        while (!month.isAfter(LAST_SEED_MONTH)) {
            MonthPlan plan = MONTH_PLANS.get(month);

            for (int extraIndex = 0; extraIndex < plan.additionalAppointments(); extraIndex++) {
                LocalDate appointmentDate = month.atDay(1 + Math.floorMod((extraIndex * 5) + month.getMonthValue(), month.lengthOfMonth()));
                createAppointmentForDate(appointmentDate, sequence + extraIndex, false, clients, servicesByCategory, professionals, summary);
            }

            sequence += 100;
            month = month.plusMonths(1);
        }
    }

    private void createAppointmentForDate(
            LocalDate appointmentDate,
            int sequence,
            boolean dailyCoverage,
            List<ClientResponse> clients,
            Map<String, List<ServiceResponse>> servicesByCategory,
            List<SeededProfessional> professionals,
            SeedSummary summary
    ) {
        MonthPlan plan = MONTH_PLANS.get(YearMonth.from(appointmentDate));
        ClientResponse client = selectAppointmentClient(clients, plan, sequence, appointmentDate);
        SeededProfessional professional = selectProfessional(professionals, sequence, appointmentDate);
        ServiceResponse service = selectServiceForProfessional(servicesByCategory, professional, sequence, appointmentDate);
        AppointmentStatus finalStatus = determineAppointmentStatus(plan, appointmentDate, sequence, dailyCoverage);

        AppointmentRequest request = new AppointmentRequest();
        request.setClientId(client.getId());
        request.setServiceId(service.getId());
        request.setPrice(calculateAppointmentPrice(service.getPrice(), plan, sequence));
        request.setProfessionalId(professional.response().getId());
        request.setProfessionalName(null);
        request.setAppointmentDate(appointmentDate);
        request.setAppointmentTime(selectAppointmentTime(sequence, dailyCoverage));
        request.setStatus(finalStatus == AppointmentStatus.CONFIRMED ? AppointmentStatus.PENDING : finalStatus);
        request.setInternalNotes(noteFrom(APPOINTMENT_NOTES, sequence));

        AppointmentResponse created = appointmentService.createAppointment(request);
        summary.appointmentsCreated++;

        if (finalStatus == AppointmentStatus.CONFIRMED) {
            request.setStatus(AppointmentStatus.CONFIRMED);
            appointmentService.updateAppointment(created.getId(), request);
            summary.automaticAppointmentSalesCreated++;
        }
    }

    private void seedManualSales(
            List<ClientResponse> clients,
            List<ServiceResponse> services,
            List<SeededInventoryProduct> inventoryProducts,
            SeedSummary summary
    ) {
        seedShowcaseManualProductSales(clients, inventoryProducts, summary);
        ensureWeeklyProductSalesCoverage(clients, inventoryProducts, summary);
        seedManualServiceSales(clients, services, summary);
    }

    private void seedShowcaseManualProductSales(
            List<ClientResponse> clients,
            List<SeededInventoryProduct> inventoryProducts,
            SeedSummary summary
    ) {
        List<SeededInventoryItem> inventoryItems = buildInventoryItems(inventoryProducts);
        List<SeededInventoryItem> showcaseItems = List.of(
                requireDemoInventoryItem(inventoryItems, "Champu L'Oreal Prof"),
                requireDemoInventoryItem(inventoryItems, "Crema Hidratante Facial"),
                requireDemoInventoryItem(inventoryItems, "Camiseta Basica - Azul / M"),
                requireDemoInventoryItem(inventoryItems, "Tornillo Galvanizado - 4mm / 30mm")
        );

        for (int index = 0; index < showcaseItems.size(); index++) {
            SeededInventoryItem item = showcaseItems.get(index);
            int quantity = item.variant() && item.itemName().startsWith("Tornillo") ? 12 : 1;

            saleService.createManualSale(
                    CreateSaleRequest.builder()
                            .clientId(resolveManualSaleClientId(clients, 30_000 + index, 50))
                            .itemType(SaleItemType.PRODUCT)
                            .inventoryProductId(item.productId())
                            .inventoryProductVariantId(item.variantId())
                            .unitPrice(calculateProductSalePrice(item.purchasePrice(), item.categoryName()))
                            .quantity(quantity)
                            .paymentMethod(paymentMethodFrom(30_000 + index))
                            .saleDate(LocalDate.of(2026, 4, 2).plusDays(index * 4L))
                            .notes("Venta demo con referencia directa de inventario.")
                            .build()
            );

            summary.manualProductSalesCreated++;
        }
    }

    private void ensureWeeklyProductSalesCoverage(
            List<ClientResponse> clients,
            List<SeededInventoryProduct> inventoryProducts,
            SeedSummary summary
    ) {
        List<SeededInventoryItem> sellableItems = buildInventoryItems(inventoryProducts).stream()
                .filter(SeededInventoryItem::sellable)
                .toList();
        Map<Long, InventoryProductEntity> productsById = inventoryProductRepository.findAll().stream()
                .filter(product -> Boolean.TRUE.equals(product.getActive()))
                .collect(Collectors.toMap(InventoryProductEntity::getId, product -> product));
        Map<Long, InventoryProductVariantEntity> variantsById = inventoryProductVariantRepository.findAll().stream()
                .filter(variant -> Boolean.TRUE.equals(variant.getActive()))
                .collect(Collectors.toMap(InventoryProductVariantEntity::getId, variant -> variant));
        Map<InventoryItemKey, Integer> sellableUnitsByItem = buildSellableUnitsMap(
                sellableItems,
                productsById,
                variantsById
        );

        LocalDate weekStart = SEED_START_DATE.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int globalIndex = 0;

        while (!weekStart.isAfter(SEED_END_DATE)) {
            if (weekStart.isBefore(SEED_START_DATE)) {
                weekStart = weekStart.plusWeeks(1);
                continue;
            }

            MonthPlan plan = MONTH_PLANS.get(YearMonth.from(weekStart.plusDays(Math.min(3, SEED_END_DATE.toEpochDay() - weekStart.toEpochDay()))));
            int weeklySales = Math.max(WEEKLY_PRODUCT_SALES_MIN, plan.productSalesPerWeek());

            for (int saleIndex = 0; saleIndex < weeklySales; saleIndex++) {
                SeededInventoryItem item = selectInventoryItemForSale(
                        sellableItems,
                        sellableUnitsByItem,
                        globalIndex + saleIndex
                );
                LocalDate saleDate = weekStart.plusDays(saleIndex % 5);
                if (saleDate.isAfter(SEED_END_DATE)) {
                    saleDate = SEED_END_DATE;
                }

                saleService.createManualSale(
                        CreateSaleRequest.builder()
                                .clientId(resolveManualSaleClientId(clients, globalIndex + saleIndex, 60))
                                .itemType(SaleItemType.PRODUCT)
                                .inventoryProductId(item.productId())
                                .inventoryProductVariantId(item.variantId())
                                .unitPrice(calculateProductSalePrice(item.purchasePrice(), item.categoryName()))
                                .quantity(1)
                                .paymentMethod(paymentMethodFrom(globalIndex + saleIndex))
                                .saleDate(saleDate)
                                .notes(noteFrom(SALE_NOTES, globalIndex + saleIndex))
                                .build()
                );

                sellableUnitsByItem.computeIfPresent(item.key(), (key, value) -> value - 1);
                summary.manualProductSalesCreated++;
            }

            summary.weeksCoveredWithProductSales++;
            weekStart = weekStart.plusWeeks(1);
            globalIndex += 10;
        }
    }

    private void seedManualServiceSales(
            List<ClientResponse> clients,
            List<ServiceResponse> services,
            SeedSummary summary
    ) {
        List<ServiceResponse> activeServices = services.stream()
                .filter(service -> Boolean.TRUE.equals(service.getActive()))
                .sorted(Comparator.comparing(ServiceResponse::getPrice).reversed())
                .toList();

        YearMonth month = FIRST_SEED_MONTH;
        int globalIndex = 20_000;

        while (!month.isAfter(LAST_SEED_MONTH)) {
            MonthPlan plan = MONTH_PLANS.get(month);

            for (int saleIndex = 0; saleIndex < plan.manualServiceSales(); saleIndex++) {
                ServiceResponse service = activeServices.get(Math.floorMod(globalIndex + saleIndex, activeServices.size()));
                LocalDate saleDate = month.atDay(1 + Math.floorMod((saleIndex * 3) + month.getMonthValue(), month.lengthOfMonth()));

                saleService.createManualSale(
                        CreateSaleRequest.builder()
                                .clientId(resolveManualSaleClientId(clients, globalIndex + saleIndex, 45))
                                .itemType(SaleItemType.SERVICE)
                                .serviceId(service.getId())
                                .unitPrice(calculateServiceSalePrice(service.getPrice(), plan, globalIndex + saleIndex))
                                .quantity(1)
                                .paymentMethod(paymentMethodFrom(globalIndex + saleIndex))
                                .saleDate(saleDate)
                                .notes(noteFrom(SALE_NOTES, globalIndex + saleIndex))
                                .build()
                );

                summary.manualServiceSalesCreated++;
            }

            globalIndex += 100;
            month = month.plusMonths(1);
        }
    }

    private void shapeEconomicScenarios(List<ServiceResponse> services, SeedSummary summary) {
        List<ServiceResponse> premiumServices = services.stream()
                .filter(service -> Boolean.TRUE.equals(service.getActive()))
                .sorted(Comparator.comparing(ServiceResponse::getPrice).reversed())
                .toList();

        applyMonthlyCorrections(premiumServices, summary);
        applyQuarterlyCorrections(premiumServices, summary);
        applyYearlyCorrections(premiumServices, summary);
    }

    private void applyMonthlyCorrections(List<ServiceResponse> premiumServices, SeedSummary summary) {
        for (MonthPlan plan : MONTH_PLANS.values()) {
            if (plan.targetOutcome() == OutcomeTarget.FLEX) {
                continue;
            }

            BigDecimal profit = calculateProfit(plan.month().atDay(1), plan.month().atEndOfMonth());
            if (plan.targetOutcome() == OutcomeTarget.GAIN && profit.compareTo(plan.targetMargin()) < 0) {
                createSupplementalServiceSales(plan.month(), plan.targetMargin().subtract(profit).add(money("20.00")), premiumServices, summary, plan.month().getMonthValue());
            }
            if (plan.targetOutcome() == OutcomeTarget.LOSS && profit.compareTo(plan.targetMargin().negate()) > 0) {
                createCorrectiveExpense(plan.month(), profit.add(plan.targetMargin()).add(money("20.00")), summary, "Ajuste operativo " + plan.month());
            }
        }
    }

    private void applyQuarterlyCorrections(List<ServiceResponse> premiumServices, SeedSummary summary) {
        for (int year = SEED_START_DATE.getYear(); year <= SEED_END_DATE.getYear(); year++) {
            for (int quarter = 1; quarter <= 4; quarter++) {
                OutcomeTarget target = targetQuarterOutcome(year, quarter);
                if (target == OutcomeTarget.FLEX) {
                    continue;
                }

                LocalDate fromDate = YearMonth.of(year, ((quarter - 1) * 3) + 1).atDay(1);
                LocalDate toDate = fromDate.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
                BigDecimal profit = calculateProfit(fromDate, toDate);

                if (target == OutcomeTarget.GAIN && profit.compareTo(targetQuarterMargin(year, quarter)) < 0) {
                    createSupplementalServiceSales(YearMonth.from(toDate), targetQuarterMargin(year, quarter).subtract(profit).add(money("35.00")), premiumServices, summary, year + quarter);
                }
                if (target == OutcomeTarget.LOSS && profit.compareTo(targetQuarterMargin(year, quarter).negate()) > 0) {
                    createCorrectiveExpense(YearMonth.from(toDate), profit.add(targetQuarterMargin(year, quarter)).add(money("35.00")), summary, "Ajuste trimestral Q" + quarter + " " + year);
                }
            }
        }
    }

    private void applyYearlyCorrections(List<ServiceResponse> premiumServices, SeedSummary summary) {
        for (int year = SEED_START_DATE.getYear(); year <= SEED_END_DATE.getYear(); year++) {
            OutcomeTarget target = targetYearOutcome(year);
            if (target == OutcomeTarget.FLEX) {
                continue;
            }

            LocalDate fromDate = LocalDate.of(year, 1, 1);
            LocalDate toDate = LocalDate.of(year, 12, 31);
            BigDecimal profit = calculateProfit(fromDate, toDate);

            if (target == OutcomeTarget.GAIN && profit.compareTo(targetYearMargin(year)) < 0) {
                createSupplementalServiceSales(YearMonth.of(year, 12), targetYearMargin(year).subtract(profit).add(money("50.00")), premiumServices, summary, year);
            }
            if (target == OutcomeTarget.LOSS && profit.compareTo(targetYearMargin(year).negate()) > 0) {
                createCorrectiveExpense(YearMonth.of(year, 12), profit.add(targetYearMargin(year)).add(money("50.00")), summary, "Ajuste anual " + year);
            }
        }
    }

    private void createSupplementalServiceSales(
            YearMonth month,
            BigDecimal requiredIncome,
            List<ServiceResponse> premiumServices,
            SeedSummary summary,
            int offset
    ) {
        BigDecimal remaining = requiredIncome.setScale(2, RoundingMode.HALF_UP);
        int index = 0;

        while (remaining.compareTo(BigDecimal.ZERO) > 0) {
            ServiceResponse service = premiumServices.get(Math.floorMod(offset + index, premiumServices.size()));
            BigDecimal unitPrice = BigDecimal.valueOf(service.getPrice()).multiply(new BigDecimal("1.08")).setScale(2, RoundingMode.HALF_UP);
            int quantity = Math.min(8, Math.max(1, remaining.divide(unitPrice, 0, RoundingMode.CEILING).intValue()));
            BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            LocalDate saleDate = month.atDay(1 + Math.floorMod((offset + index) * 2, month.lengthOfMonth()));

            saleService.createManualSale(
                    CreateSaleRequest.builder()
                            .clientId(null)
                            .itemType(SaleItemType.SERVICE)
                            .serviceId(service.getId())
                            .unitPrice(unitPrice)
                            .quantity(quantity)
                            .paymentMethod(paymentMethodFrom(offset + index))
                            .saleDate(saleDate)
                            .notes("Ajuste dirigido del seed para reforzar escenario economico.")
                            .build()
            );

            summary.manualServiceSalesCreated++;
            summary.correctiveServiceSalesCreated++;
            remaining = remaining.subtract(totalAmount);
            index++;
        }
    }

    private void createCorrectiveExpense(YearMonth month, BigDecimal amount, SeedSummary summary, String label) {
        BigDecimal remaining = amount.setScale(2, RoundingMode.HALF_UP);
        int index = 0;

        while (remaining.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal chunk = remaining.min(money("6500.00"));
            ExpenseCategory category = index % 2 == 0 ? ExpenseCategory.OTHER : ExpenseCategory.SERVICES;

            expenseService.createExpense(
                    CreateExpenseRequest.builder()
                            .expenseDate(resolveExpenseDate(month, 24 + Math.min(index, 4)))
                            .concept(correctiveExpenseConcept(label, index))
                            .category(category)
                            .quantity(1)
                            .amount(chunk)
                            .notes("Ajuste dirigido del seed para asegurar diversidad economica.")
                            .build()
            );

            summary.manualExpensesCreated++;
            summary.correctiveExpensesCreated++;
            remaining = remaining.subtract(chunk);
            index++;
        }
    }

    private ClientResponse createClient(String fullName, String phone, String email) {
        ClientRequest request = new ClientRequest();
        request.setFullName(fullName);
        request.setPhone(phone);
        request.setEmail(email);
        return clientService.createClient(request);
    }

    private ClientResponse selectAppointmentClient(
            List<ClientResponse> clients,
            MonthPlan plan,
            int sequence,
            LocalDate appointmentDate
    ) {
        if (Math.floorMod(sequence * 19 + appointmentDate.getDayOfYear(), 100) < plan.walkInPercent()) {
            return clients.getFirst();
        }

        return clients.get(1 + Math.floorMod(sequence * 7 + appointmentDate.getDayOfMonth(), clients.size() - 1));
    }

    private SeededProfessional selectProfessional(List<SeededProfessional> professionals, int sequence, LocalDate appointmentDate) {
        List<SeededProfessional> activeProfessionals = professionals.stream()
                .filter(professional -> Boolean.TRUE.equals(professional.response().getActive()))
                .toList();

        return activeProfessionals.get(Math.floorMod(sequence + appointmentDate.getDayOfWeek().getValue(), activeProfessionals.size()));
    }

    private ServiceResponse selectServiceForProfessional(
            Map<String, List<ServiceResponse>> servicesByCategory,
            SeededProfessional professional,
            int sequence,
            LocalDate appointmentDate
    ) {
        List<ServiceResponse> preferredServices = professional.seed().preferredCategories().stream()
                .map(servicesByCategory::get)
                .filter(list -> list != null && !list.isEmpty())
                .flatMap(List::stream)
                .toList();

        if (!preferredServices.isEmpty() && Math.floorMod(sequence * 11 + appointmentDate.getDayOfMonth(), 100) < 72) {
            return preferredServices.get(Math.floorMod(sequence * 3 + appointmentDate.getMonthValue(), preferredServices.size()));
        }

        List<ServiceResponse> allServices = servicesByCategory.values().stream().flatMap(List::stream).toList();
        return allServices.get(Math.floorMod(sequence * 5 + appointmentDate.getDayOfYear(), allServices.size()));
    }

    private AppointmentStatus determineAppointmentStatus(
            MonthPlan plan,
            LocalDate appointmentDate,
            int sequence,
            boolean dailyCoverage
    ) {
        if (dailyCoverage && appointmentDate.getDayOfMonth() == 1) {
            return AppointmentStatus.CONFIRMED;
        }

        if (appointmentDate.equals(LocalDate.now())) {
            return Math.floorMod(sequence, 3) == 0 ? AppointmentStatus.PENDING : AppointmentStatus.CONFIRMED;
        }

        int bucket = Math.floorMod(sequence * 37 + appointmentDate.getDayOfYear(), 100);
        if (bucket < plan.confirmedPercent()) {
            return AppointmentStatus.CONFIRMED;
        }
        if (bucket < plan.confirmedPercent() + plan.cancelledPercent()) {
            return AppointmentStatus.CANCELLED;
        }
        return AppointmentStatus.PENDING;
    }

    private LocalTime selectAppointmentTime(int sequence, boolean dailyCoverage) {
        int multiplier = dailyCoverage ? 2 : 5;
        return APPOINTMENT_TIME_SLOTS.get(Math.floorMod(sequence * multiplier + (dailyCoverage ? 1 : 3), APPOINTMENT_TIME_SLOTS.size()));
    }

    private Double calculateAppointmentPrice(Double basePrice, MonthPlan plan, int sequence) {
        BigDecimal price = BigDecimal.valueOf(basePrice)
                .multiply(plan.appointmentPriceFactor())
                .multiply(sequence % 9 == 0 ? new BigDecimal("0.96") : BigDecimal.ONE);

        return price.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal calculateServiceSalePrice(Double basePrice, MonthPlan plan, int sequence) {
        BigDecimal price = BigDecimal.valueOf(basePrice)
                .multiply(plan.manualServicePriceFactor())
                .multiply(sequence % 8 == 0 ? new BigDecimal("0.94") : BigDecimal.ONE);

        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateProductSalePrice(BigDecimal purchasePrice, String categoryName) {
        BigDecimal markup = switch (categoryName) {
            case "Retail Profesional", "Tratamientos Capilares", "Maquillaje" -> new BigDecimal("2.10");
            case "Barberia", "Estetica Facial", "Unas" -> new BigDecimal("2.00");
            default -> new BigDecimal("1.85");
        };

        return purchasePrice.multiply(markup).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<InventoryItemKey, Integer> buildSellableUnitsMap(
            List<SeededInventoryItem> sellableItems,
            Map<Long, InventoryProductEntity> productsById,
            Map<Long, InventoryProductVariantEntity> variantsById
    ) {
        Map<InventoryItemKey, Integer> sellableUnits = new HashMap<>();
        int totalSellableUnits = 0;

        for (SeededInventoryItem item : sellableItems) {
            StockSnapshot stockSnapshot = resolveCurrentStockSnapshot(item, productsById, variantsById);
            if (stockSnapshot == null) {
                continue;
            }

            int protectedStock = calculateProtectedStock(item.stockProfile(), stockSnapshot.stock(), stockSnapshot.minimumStock());

            int sellable = Math.max(0, stockSnapshot.stock() - protectedStock);
            sellableUnits.put(item.key(), sellable);
            totalSellableUnits += sellable;
        }

        int requiredUnits = summaryWeeksBetween() * WEEKLY_PRODUCT_SALES_MIN;
        if (totalSellableUnits < requiredUnits) {
            throw new IllegalStateException("No hay stock vendible suficiente para garantizar las ventas de producto semanales");
        }

        return sellableUnits;
    }

    private StockSnapshot resolveCurrentStockSnapshot(
            SeededInventoryItem item,
            Map<Long, InventoryProductEntity> productsById,
            Map<Long, InventoryProductVariantEntity> variantsById
    ) {
        if (item.variant()) {
            InventoryProductVariantEntity variant = variantsById.get(item.variantId());
            return variant == null ? null : new StockSnapshot(variant.getStock(), variant.getMinimumStock());
        }

        InventoryProductEntity product = productsById.get(item.productId());
        return product == null ? null : new StockSnapshot(product.getStock(), product.getMinimumStock());
    }

    private int calculateProtectedStock(StockProfile stockProfile, int stock, int minimumStock) {
        return switch (stockProfile) {
            case OUT -> stock;
            case LOW -> Math.max(0, minimumStock - 1);
            case BALANCED -> minimumStock + 2;
            case HIGH -> minimumStock + 8;
        };
    }

    private SeededInventoryItem selectInventoryItemForSale(
            List<SeededInventoryItem> sellableItems,
            Map<InventoryItemKey, Integer> sellableUnitsByItem,
            int sequence
    ) {
        int startIndex = Math.floorMod(sequence * 7, sellableItems.size());

        for (int offset = 0; offset < sellableItems.size(); offset++) {
            SeededInventoryItem item = sellableItems.get((startIndex + offset) % sellableItems.size());
            if (sellableUnitsByItem.getOrDefault(item.key(), 0) > 0) {
                return item;
            }
        }

        throw new IllegalStateException("No quedan productos con stock vendible para completar el seed");
    }

    private Long resolveManualSaleClientId(List<ClientResponse> clients, int sequence, int walkInPercent) {
        if (Math.floorMod(sequence * 13, 100) < walkInPercent) {
            return null;
        }

        return clients.get(1 + Math.floorMod(sequence * 5, clients.size() - 1)).getId();
    }

    private BigDecimal calculateRecurringExpenseAmount(BigDecimal baseAmount, MonthPlan plan, YearMonth month) {
        BigDecimal seasonalFactor = switch (month.getMonthValue()) {
            case 1, 2, 7, 8, 12 -> new BigDecimal("1.08");
            case 4, 5, 10 -> new BigDecimal("0.97");
            default -> BigDecimal.ONE;
        };

        return baseAmount
                .multiply(plan.recurringExpenseMultiplier())
                .multiply(seasonalFactor)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateProfit(LocalDate fromDate, LocalDate toDate) {
        ReportPeriodTotals totals = reportQueryRepository.findPeriodTotals(fromDate, toDate);
        return totals.totalSales().subtract(totals.totalExpenses()).setScale(2, RoundingMode.HALF_UP);
    }

    private OutcomeTarget targetQuarterOutcome(int year, int quarter) {
        return switch (year) {
            case 2024 -> quarter == 3 ? OutcomeTarget.LOSS : OutcomeTarget.GAIN;
            case 2025 -> quarter == 3 ? OutcomeTarget.GAIN : OutcomeTarget.LOSS;
            case 2026 -> quarter % 2 == 0 ? OutcomeTarget.LOSS : OutcomeTarget.GAIN;
            case 2027 -> quarter == 2 ? OutcomeTarget.LOSS : OutcomeTarget.GAIN;
            default -> OutcomeTarget.FLEX;
        };
    }

    private BigDecimal targetQuarterMargin(int year, int quarter) {
        return switch (year) {
            case 2024 -> quarter == 3 ? money("420.00") : money("720.00");
            case 2025 -> quarter == 3 ? money("360.00") : money("860.00");
            case 2026 -> quarter % 2 == 0 ? money("420.00") : money("560.00");
            case 2027 -> quarter == 2 ? money("380.00") : money("900.00");
            default -> money("250.00");
        };
    }

    private OutcomeTarget targetYearOutcome(int year) {
        return switch (year) {
            case 2024, 2027 -> OutcomeTarget.GAIN;
            case 2025 -> OutcomeTarget.LOSS;
            case 2026 -> OutcomeTarget.FLEX;
            default -> OutcomeTarget.FLEX;
        };
    }

    private BigDecimal targetYearMargin(int year) {
        return switch (year) {
            case 2024 -> money("1600.00");
            case 2025 -> money("2600.00");
            case 2027 -> money("2200.00");
            default -> money("300.00");
        };
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

    private String correctiveExpenseConcept(String label, int index) {
        List<String> concepts = List.of(
                "Reforma del local",
                "Compra de maquinaria",
                "Camillas nuevas de cabina",
                "Lamparas profesionales",
                "Ordenadores y TPV",
                "Consultoria externa",
                "Mantenimiento extraordinario",
                "Campana publicitaria intensiva"
        );
        return concepts.get(Math.floorMod(label.hashCode() + index, concepts.size()));
    }

    private LocalDate resolveExpenseDate(YearMonth month, int dayOfMonth) {
        return month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
    }

    private StockProfile resolveStockProfile(int index) {
        return switch (index % 10) {
            case 0, 5 -> StockProfile.OUT;
            case 1, 6 -> StockProfile.LOW;
            case 2, 7 -> StockProfile.BALANCED;
            default -> StockProfile.HIGH;
        };
    }

    private int resolveInitialStock(StockProfile profile, int minimumStock, int index) {
        return switch (profile) {
            case OUT -> 0;
            case LOW -> Math.max(1, minimumStock - 1);
            case BALANCED -> minimumStock + 2 + (index % 4);
            case HIGH -> minimumStock + 18 + (index % 15);
        };
    }

    private StockProfile resolveStockProfile(int stock, int minimumStock) {
        if (stock == 0) {
            return StockProfile.OUT;
        }
        if (stock <= minimumStock) {
            return StockProfile.LOW;
        }
        if (stock <= minimumStock + 8) {
            return StockProfile.BALANCED;
        }
        return StockProfile.HIGH;
    }

    private String buildInventoryDescription(String categoryName, String productName, boolean sellable) {
        if (sellable) {
            return "Producto de venta y reposicion para " + categoryName + ": " + productName + ".";
        }
        return "Consumible tecnico de uso profesional en " + categoryName + ": " + productName + ".";
    }

    private String buildSku(String categoryName, int index) {
        String normalizedCategory = normalizeToken(categoryName);
        String prefix = normalizedCategory.substring(0, Math.min(3, normalizedCategory.length())).toUpperCase(Locale.ROOT);
        return "DEM-" + prefix + "-" + String.format("%04d", index + 1);
    }

    private String buildClientFullName(int sequence) {
        String firstName = CLIENT_FIRST_NAMES.get(Math.floorMod(sequence * 3, CLIENT_FIRST_NAMES.size()));
        String lastNameOne = CLIENT_LAST_NAMES.get(Math.floorMod(sequence * 5, CLIENT_LAST_NAMES.size()));
        String lastNameTwo = CLIENT_LAST_NAMES.get(Math.floorMod(sequence * 7 + 3, CLIENT_LAST_NAMES.size()));
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

    private PaymentMethod paymentMethodFrom(int sequence) {
        return PAYMENT_METHODS.get(Math.floorMod(sequence, PAYMENT_METHODS.size()));
    }

    private String noteFrom(List<String> notes, int sequence) {
        return notes.get(Math.floorMod(sequence, notes.size()));
    }

    private int summaryWeeksBetween() {
        return (int) ((SEED_END_DATE.toEpochDay() - SEED_START_DATE.toEpochDay()) / 7L) + 1;
    }

    private String normalizeToken(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace(" ", "-")
                .replace(".", "")
                .replace(",", "")
                .replace("'", "");
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
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

    private static List<CategorySeed> buildCategorySeeds() {
        return List.of(
                new CategorySeed("Corte y Peinado", "rose", "content_cut"),
                new CategorySeed("Coloracion", "amber", "palette"),
                new CategorySeed("Barberia", "slate", "shield"),
                new CategorySeed("Unas", "pink", "auto_awesome"),
                new CategorySeed("Estetica Facial", "emerald", "local_florist"),
                new CategorySeed("Depilacion", "sand", "wb_sunny"),
                new CategorySeed("Masajes y Bienestar", "teal", "spa"),
                new CategorySeed("Tratamientos Capilares", "cyan", "water_drop"),
                new CategorySeed("Maquillaje", "red", "brush"),
                new CategorySeed("Retail Profesional", "indigo", "shopping_bag")
        );
    }

    private static List<ServiceSeed> buildServiceSeeds() {
        return List.of(
                new ServiceSeed("Corte femenino express", "Corte de mantenimiento con acabado natural.", 45, 24.00, true, "Corte y Peinado"),
                new ServiceSeed("Lavado y brushing pulido", "Lavado completo con secado trabajado.", 45, 23.00, true, "Corte y Peinado"),
                new ServiceSeed("Peinado ondas suaves", "Ondas abiertas para evento o diario.", 60, 29.00, true, "Corte y Peinado"),
                new ServiceSeed("Recogido invitada", "Recogido elegante para celebraciones.", 75, 48.00, true, "Corte y Peinado"),
                new ServiceSeed("Corte infantil", "Servicio rapido para peques.", 30, 15.00, true, "Corte y Peinado"),
                new ServiceSeed("Color raiz", "Cobertura de raiz y uniformidad de tono.", 90, 41.00, true, "Coloracion"),
                new ServiceSeed("Bano de color brillo", "Refresco de color y brillo global.", 75, 46.00, true, "Coloracion"),
                new ServiceSeed("Matiz corrector", "Correccion de reflejo y mantenimiento.", 45, 28.00, true, "Coloracion"),
                new ServiceSeed("Mechas babylights", "Aclarado fino y luminoso.", 150, 102.00, true, "Coloracion"),
                new ServiceSeed("Balayage completo", "Tecnica degradada con acabado premium.", 180, 148.00, true, "Coloracion"),
                new ServiceSeed("Color fantasia", "Aplicacion de tonos creativos personalizados.", 150, 118.00, false, "Coloracion"),
                new ServiceSeed("Ritual plex post color", "Tratamiento reparador tras trabajo tecnico.", 45, 34.00, true, "Coloracion"),
                new ServiceSeed("Corte caballero tradicional", "Corte clasico a tijera o maquina.", 30, 17.00, true, "Barberia"),
                new ServiceSeed("Corte fade premium", "Degradado tecnico con lavado.", 45, 25.00, true, "Barberia"),
                new ServiceSeed("Arreglo de barba", "Perfilado y forma de barba.", 30, 16.00, true, "Barberia"),
                new ServiceSeed("Afeitado clasico", "Afeitado con toalla caliente.", 30, 19.00, true, "Barberia"),
                new ServiceSeed("Manicura semipermanente", "Preparacion, esmalte y sellado.", 60, 27.00, true, "Unas"),
                new ServiceSeed("Pedicura spa", "Pedicura completa con exfoliacion.", 75, 35.00, true, "Unas"),
                new ServiceSeed("Retirada y refuerzo", "Retirada de producto anterior y refuerzo.", 45, 20.00, true, "Unas"),
                new ServiceSeed("Nail art premium", "Decoracion avanzada personalizada.", 60, 30.00, false, "Unas"),
                new ServiceSeed("Limpieza facial express", "Puesta a punto rapida del rostro.", 40, 30.00, true, "Estetica Facial"),
                new ServiceSeed("Higiene facial profunda", "Limpieza completa con extraccion.", 75, 52.00, true, "Estetica Facial"),
                new ServiceSeed("Lifting de pestanas", "Curvado y elevacion de pestanas.", 60, 39.00, true, "Estetica Facial"),
                new ServiceSeed("Diseno de cejas", "Definicion de ceja con acabado limpio.", 30, 19.00, true, "Estetica Facial"),
                new ServiceSeed("Tratamiento glow cabina", "Ritual iluminador en cabina.", 60, 58.00, true, "Estetica Facial"),
                new ServiceSeed("Depilacion cejas", "Depilacion precisa de cejas.", 20, 12.00, true, "Depilacion"),
                new ServiceSeed("Depilacion labio", "Servicio rapido de depilacion facial.", 15, 8.00, true, "Depilacion"),
                new ServiceSeed("Piernas completas", "Depilacion de piernas completas.", 45, 24.00, true, "Depilacion"),
                new ServiceSeed("Axilas y ingles", "Pack de depilacion corporal.", 30, 21.00, true, "Depilacion"),
                new ServiceSeed("Masaje relajante espalda", "Descarga y relajacion de espalda.", 45, 36.00, true, "Masajes y Bienestar"),
                new ServiceSeed("Masaje descontracturante", "Trabajo profundo muscular.", 60, 48.00, true, "Masajes y Bienestar"),
                new ServiceSeed("Ritual piernas ligeras", "Masaje drenante y alivio de piernas.", 45, 39.00, true, "Masajes y Bienestar"),
                new ServiceSeed("Masaje velas calientes", "Experiencia sensorial premium.", 60, 56.00, true, "Masajes y Bienestar"),
                new ServiceSeed("Tratamiento hidratacion intensa", "Hidratacion profunda del cabello.", 45, 32.00, true, "Tratamientos Capilares"),
                new ServiceSeed("Ritual reparacion keratina", "Tratamiento reparador de fibra.", 60, 44.00, true, "Tratamientos Capilares"),
                new ServiceSeed("Exfoliacion cuero cabelludo", "Limpieza tecnica del cuero cabelludo.", 30, 22.00, true, "Tratamientos Capilares"),
                new ServiceSeed("Botox capilar", "Tratamiento de nutricion avanzada.", 90, 74.00, true, "Tratamientos Capilares"),
                new ServiceSeed("Maquillaje dia", "Maquillaje ligero y luminoso.", 45, 34.00, true, "Maquillaje"),
                new ServiceSeed("Maquillaje noche", "Acabado mas intenso para evento.", 60, 48.00, true, "Maquillaje"),
                new ServiceSeed("Maquillaje novia prueba", "Prueba de maquillaje para boda.", 90, 72.00, false, "Maquillaje")
        );
    }

    private static List<ProfessionalSeed> buildProfessionalSeeds() {
        return List.of(
                new ProfessionalSeed("Lucia Torres", "Colorista senior", "611220301", "lucia.torres@demo-pymes.es", true, List.of("Coloracion", "Tratamientos Capilares")),
                new ProfessionalSeed("Marina Vega", "Tecnica en unas", "611220302", "marina.vega@demo-pymes.es", true, List.of("Unas", "Maquillaje")),
                new ProfessionalSeed("Sergio Lozano", "Barbero", "611220303", "sergio.lozano@demo-pymes.es", true, List.of("Barberia", "Corte y Peinado")),
                new ProfessionalSeed("Clara Benitez", "Esteticista facial", "611220304", "clara.benitez@demo-pymes.es", true, List.of("Estetica Facial", "Depilacion")),
                new ProfessionalSeed("Paula Rios", "Masajista", "611220305", "paula.rios@demo-pymes.es", true, List.of("Masajes y Bienestar")),
                new ProfessionalSeed("Eva Molina", "Estilista", "611220306", "eva.molina@demo-pymes.es", true, List.of("Corte y Peinado", "Tratamientos Capilares")),
                new ProfessionalSeed("Diego Salas", "Apoyo de cabina", "611220307", "diego.salas@demo-pymes.es", false, List.of("Depilacion", "Estetica Facial"))
        );
    }

    private static List<RecurringExpenseTemplate> buildRecurringExpenseTemplates() {
        return List.of(
                new RecurringExpenseTemplate("Alquiler local", ExpenseCategory.OTHER, 1, money("1650.00"), 1),
                new RecurringExpenseTemplate("Electricidad mensual", ExpenseCategory.OTHER, 1, money("320.00"), 5),
                new RecurringExpenseTemplate("Agua y saneamiento", ExpenseCategory.OTHER, 1, money("96.00"), 7),
                new RecurringExpenseTemplate("Internet y telefonia", ExpenseCategory.SERVICES, 1, money("79.00"), 9),
                new RecurringExpenseTemplate("Gestoria laboral", ExpenseCategory.SERVICES, 1, money("185.00"), 12),
                new RecurringExpenseTemplate("Marketing digital", ExpenseCategory.SERVICES, 1, money("165.00"), 16),
                new RecurringExpenseTemplate("Mantenimiento y limpieza", ExpenseCategory.SERVICES, 2, money("228.00"), 22)
        );
    }

    private static List<StrategicExpensePlan> buildStrategicExpensePlans() {
        return List.of(
                new StrategicExpensePlan(YearMonth.of(2024, 3), "Campana de lanzamiento de temporada", ExpenseCategory.SERVICES, 1, money("1800.00"), 14, "Accion comercial potente para captar clientela nueva."),
                new StrategicExpensePlan(YearMonth.of(2025, 2), "Reforma del local", ExpenseCategory.OTHER, 1, money("12500.00"), 11, "Obra integral para renovar recepcion y cabinas."),
                new StrategicExpensePlan(YearMonth.of(2025, 5), "Maquinaria de laser diodo", ExpenseCategory.OTHER, 1, money("7200.00"), 18, "Nueva aparatologia para depilacion y cabina."),
                new StrategicExpensePlan(YearMonth.of(2025, 7), "Camillas electricas cabina", ExpenseCategory.OTHER, 1, money("3850.00"), 9, "Renovacion de equipamiento de cabina."),
                new StrategicExpensePlan(YearMonth.of(2025, 9), "Lamparas profesionales", ExpenseCategory.OTHER, 1, money("1680.00"), 23, "Iluminacion tecnica para puestos de trabajo."),
                new StrategicExpensePlan(YearMonth.of(2025, 11), "Ordenadores y TPV nuevos", ExpenseCategory.OTHER, 1, money("2450.00"), 20, "Actualizacion completa del puesto de recepcion."),
                new StrategicExpensePlan(YearMonth.of(2026, 4), "Tocadores y secadores profesionales", ExpenseCategory.OTHER, 1, money("5200.00"), 13, "Mejora de puestos en sala principal."),
                new StrategicExpensePlan(YearMonth.of(2026, 6), "Reparacion climatizacion integral", ExpenseCategory.SERVICES, 1, money("3100.00"), 8, "Reparacion y mantenimiento profundo del sistema de clima."),
                new StrategicExpensePlan(YearMonth.of(2027, 2), "Renovacion de imagen de marca", ExpenseCategory.SERVICES, 1, money("1450.00"), 17, "Actualizacion de rotulacion y material visual."),
                new StrategicExpensePlan(YearMonth.of(2027, 10), "Formacion avanzada del equipo", ExpenseCategory.SERVICES, 2, money("2200.00"), 15, "Capacitacion premium en tecnicas avanzadas.")
        );
    }

    private static List<String> buildClientFirstNames() {
        return List.of(
                "Lucia", "Carmen", "Paula", "Alba", "Nuria", "Elena", "Sara", "Noelia", "Irene", "Claudia",
                "Silvia", "Laura", "Marta", "Raquel", "Bea", "Sonia", "Natalia", "Monica", "Alicia", "Rocio",
                "Daniel", "Javier", "Diego", "Mario", "Pablo", "Adrian", "Hector", "Victor", "Ruben", "Marcos",
                "Teresa", "Candela", "Patricia", "Julia", "Aitana", "Miriam", "Andrea", "Ainhoa", "Lidia", "Esther"
        );
    }

    private static List<String> buildClientLastNames() {
        return List.of(
                "Garcia", "Lopez", "Martinez", "Sanchez", "Perez", "Gomez", "Martin", "Jimenez", "Ruiz", "Hernandez",
                "Diaz", "Moreno", "Munoz", "Alonso", "Romero", "Navarro", "Torres", "Dominguez", "Vazquez", "Ramos",
                "Gil", "Serrano", "Blanco", "Molina", "Castro", "Ortega", "Delgado", "Suarez", "Benitez", "Prieto",
                "Campos", "Vega", "Lorenzo", "Calvo", "Carrasco", "Bravo", "Marin", "Iglesias", "Flores", "Fuentes"
        );
    }

    private static List<String> buildAppointmentNotes() {
        return List.of(
                "Primera visita del cliente.",
                "Prefiere horario de manana.",
                "Solicita acabado natural.",
                "Llega con tiempo justo desde el trabajo.",
                "Revisar historial de color anterior.",
                "Recordar oferta de mantenimiento en casa.",
                "Cliente habitual del equipo.",
                "Quiere acabar antes del mediodia."
        );
    }

    private static List<String> buildExpenseNotes() {
        return List.of(
                "Registrado para control mensual.",
                "Compra recurrente de operativa diaria.",
                "Facturado por proveedor habitual.",
                "Necesario para mantener el servicio activo.",
                "Ajuste de aprovisionamiento del mes.",
                "Gasto incluido en la planificacion del seed."
        );
    }

    private static List<String> buildSaleNotes() {
        return List.of(
                "Venta registrada en recepcion.",
                "Cliente aprovecha recomendacion del equipo.",
                "Cobro habitual al cierre de servicio.",
                "Operacion para reporte comercial.",
                "Venta de mostrador.",
                "Venta vinculada a recomendacion de mantenimiento."
        );
    }

    private static Map<YearMonth, MonthPlan> buildMonthPlans() {
        Map<YearMonth, MonthPlan> plans = new LinkedHashMap<>();
        YearMonth month = FIRST_SEED_MONTH;

        while (!month.isAfter(LAST_SEED_MONTH)) {
            plans.put(month, buildMonthPlan(month));
            month = month.plusMonths(1);
        }

        return Collections.unmodifiableMap(plans);
    }

    private static MonthPlan buildMonthPlan(YearMonth month) {
        OutcomeTarget quarterOutcome = switch (month.getYear()) {
            case 2024 -> month.getMonthValue() >= 7 && month.getMonthValue() <= 9 ? OutcomeTarget.LOSS : OutcomeTarget.GAIN;
            case 2025 -> month.getMonthValue() >= 7 && month.getMonthValue() <= 9 ? OutcomeTarget.GAIN : OutcomeTarget.LOSS;
            case 2026 -> ((month.getMonthValue() - 1) / 3) % 2 == 0 ? OutcomeTarget.GAIN : OutcomeTarget.LOSS;
            case 2027 -> month.getMonthValue() >= 4 && month.getMonthValue() <= 6 ? OutcomeTarget.LOSS : OutcomeTarget.GAIN;
            default -> OutcomeTarget.FLEX;
        };

        int monthInQuarter = ((month.getMonthValue() - 1) % 3) + 1;
        OutcomeTarget monthOutcome = quarterOutcome == OutcomeTarget.GAIN
                ? (monthInQuarter == 2 ? OutcomeTarget.LOSS : OutcomeTarget.GAIN)
                : (monthInQuarter == 2 ? OutcomeTarget.GAIN : OutcomeTarget.LOSS);

        return switch (month.getYear()) {
            case 2024 -> monthOutcome == OutcomeTarget.GAIN
                    ? new MonthPlan(month, 16 + (month.getMonthValue() % 5), 66, 14, 42, 6, 10, new BigDecimal("1.03"), new BigDecimal("1.02"), new BigDecimal("0.94"), monthOutcome, money("180.00"))
                    : new MonthPlan(month, 8 + (month.getMonthValue() % 3), 48, 18, 55, 5, 5, new BigDecimal("0.97"), new BigDecimal("0.96"), new BigDecimal("1.12"), monthOutcome, money("240.00"));
            case 2025 -> monthOutcome == OutcomeTarget.GAIN
                    ? new MonthPlan(month, 12 + (month.getMonthValue() % 4), 58, 14, 46, 6, 9, new BigDecimal("1.01"), new BigDecimal("1.00"), new BigDecimal("1.02"), monthOutcome, money("160.00"))
                    : new MonthPlan(month, 5 + (month.getMonthValue() % 3), 38, 22, 60, 5, 4, new BigDecimal("0.95"), new BigDecimal("0.94"), new BigDecimal("1.22"), monthOutcome, money("280.00"));
            case 2026 -> monthOutcome == OutcomeTarget.GAIN
                    ? new MonthPlan(month, 14 + (month.getMonthValue() % 4), 61, 16, 48, 6, 8, new BigDecimal("1.02"), new BigDecimal("1.01"), new BigDecimal("0.98"), monthOutcome, money("170.00"))
                    : new MonthPlan(month, 7 + (month.getMonthValue() % 3), 44, 20, 57, 5, 5, new BigDecimal("0.98"), new BigDecimal("0.97"), new BigDecimal("1.10"), monthOutcome, money("220.00"));
            case 2027 -> monthOutcome == OutcomeTarget.GAIN
                    ? new MonthPlan(month, 22 + (month.getMonthValue() % 5), 72, 11, 40, 7, 14, new BigDecimal("1.07"), new BigDecimal("1.05"), new BigDecimal("0.92"), monthOutcome, money("240.00"))
                    : new MonthPlan(month, 10 + (month.getMonthValue() % 4), 50, 18, 53, 5, 7, new BigDecimal("0.99"), new BigDecimal("0.98"), new BigDecimal("1.08"), monthOutcome, money("190.00"));
            default -> new MonthPlan(month, 10, 55, 15, 50, 5, 6, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, OutcomeTarget.FLEX, money("0.00"));
        };
    }

    private static List<InventoryTemplate> buildInventoryTemplates() {
        List<InventoryTemplate> templates = new ArrayList<>(TARGET_INVENTORY_PRODUCTS);

        addFeaturedInventoryTemplates(templates);

        addInventoryTemplates(
                templates,
                "Retail Profesional",
                true,
                List.of("Champu", "Acondicionador", "Mascarilla", "Serum", "Protector termico"),
                List.of("argan repair", "color shield", "rizo definido", "volumen ligero", "keratina smooth", "brillo espejo"),
                money("7.10")
        );
        addInventoryTemplates(
                templates,
                "Coloracion",
                false,
                List.of("Tinte permanente", "Oxidante crema", "Matizador", "Decolorante", "Plex tecnico"),
                List.of("4.0 castano", "5.35 avellana", "6.34 miel", "7.1 ceniza", "perla frio", "beige natural"),
                money("4.20")
        );
        addInventoryTemplates(
                templates,
                "Barberia",
                true,
                List.of("Aceite barba", "Balsamo after shave", "Cera mate", "Pomada brillo", "Gel afeitado"),
                List.of("cedar", "mint fresh", "amber wood", "classic barber", "sensitive skin", "urban hold"),
                money("5.40")
        );
        addInventoryTemplates(
                templates,
                "Estetica Facial",
                true,
                List.of("Gel limpiador", "Tonico calmante", "Serum", "Crema nutritiva", "Mascara facial"),
                List.of("vitamina c", "aloe balance", "hialuronico", "niacinamida", "calendula", "glow repair"),
                money("8.20")
        );
        addInventoryTemplates(
                templates,
                "Unas",
                true,
                List.of("Base rubber", "Top coat", "Esmalte semipermanente", "Aceite cuticulas", "Gel constructor"),
                List.of("nude sand", "rosa clean", "rojo cereza", "moka soft", "milky white", "peach nude"),
                money("4.60")
        );
        addInventoryTemplates(
                templates,
                "Masajes y Bienestar",
                false,
                List.of("Aceite masaje", "Crema drenante", "Gel frio", "Locion relajante"),
                List.of("lavanda", "romero", "citricos", "arnica", "eucalipto", "relax deep"),
                money("6.70")
        );
        addInventoryTemplates(
                templates,
                "Tratamientos Capilares",
                true,
                List.of("Ampolla capilar", "Locion cuero cabelludo", "Exfoliante capilar", "Ritual reparador"),
                List.of("anti caida", "hidratacion pro", "detox scalp", "densidad boost", "repair intense", "shine pro"),
                money("7.80")
        );
        addInventoryTemplates(
                templates,
                "Maquillaje",
                true,
                List.of("Base fluida", "Corrector", "Iluminador", "Fijador maquillaje"),
                List.of("nude ivory", "beige warm", "sun glow", "rose light", "soft matte", "long wear"),
                money("7.40")
        );

        return List.copyOf(templates.stream().limit(TARGET_INVENTORY_PRODUCTS).toList());
    }

    private static void addFeaturedInventoryTemplates(List<InventoryTemplate> templates) {
        templates.add(InventoryTemplate.simple(
                "Champu L'Oreal Prof",
                "Retail Profesional",
                money("7.90"),
                true,
                45,
                10,
                "DEM-RTL-CHAMP-001"
        ));
        templates.add(InventoryTemplate.simple(
                "Acondicionador Kerastase",
                "Retail Profesional",
                money("9.40"),
                true,
                36,
                8,
                "DEM-RTL-ACOND-001"
        ));
        templates.add(InventoryTemplate.simple(
                "Laca Fijacion Fuerte",
                "Retail Profesional",
                money("5.20"),
                true,
                28,
                6,
                "DEM-RTL-LACA-001"
        ));
        templates.add(InventoryTemplate.simple(
                "Crema Hidratante Facial",
                "Estetica Facial",
                money("8.60"),
                true,
                30,
                6,
                "DEM-EST-CREMA-001"
        ));
        templates.add(InventoryTemplate.simple(
                "Aceite Esencial Lavanda",
                "Masajes y Bienestar",
                money("6.70"),
                true,
                22,
                5,
                "DEM-MAS-ACEITE-001"
        ));
        templates.add(InventoryTemplate.simple(
                "Mascara Facial Vitamina C",
                "Estetica Facial",
                money("8.95"),
                true,
                26,
                6,
                "DEM-EST-MASC-001"
        ));
        templates.add(InventoryTemplate.simple(
                "Aceite Barba Cedar",
                "Barberia",
                money("5.80"),
                true,
                24,
                5,
                "DEM-BAR-ACEITE-001"
        ));
        templates.add(InventoryTemplate.simple(
                "Esmalte Semipermanente Nude",
                "Unas",
                money("4.90"),
                true,
                32,
                8,
                "DEM-UNA-ESMALTE-001"
        ));
        templates.add(InventoryTemplate.withVariants(
                "Camiseta Basica",
                "Retail Profesional",
                true,
                List.of(
                        InventoryVariantTemplate.of("Roja / S", "DEM-CAM-ROJ-S", 34, 6, money("6.80"), Map.of("color", "Roja", "talla", "S")),
                        InventoryVariantTemplate.of("Roja / M", "DEM-CAM-ROJ-M", 38, 8, money("6.80"), Map.of("color", "Roja", "talla", "M")),
                        InventoryVariantTemplate.of("Azul / M", "DEM-CAM-AZU-M", 32, 8, money("6.90"), Map.of("color", "Azul", "talla", "M")),
                        InventoryVariantTemplate.of("Azul / L", "DEM-CAM-AZU-L", 28, 6, money("6.90"), Map.of("color", "Azul", "talla", "L"))
                )
        ));
        templates.add(InventoryTemplate.withVariants(
                "Tornillo Galvanizado",
                "Retail Profesional",
                true,
                List.of(
                        InventoryVariantTemplate.of("4mm / 20mm", "DEM-TOR-4-20", 90, 15, money("0.12"), Map.of("diametro", "4mm", "largo", "20mm")),
                        InventoryVariantTemplate.of("4mm / 30mm", "DEM-TOR-4-30", 84, 15, money("0.14"), Map.of("diametro", "4mm", "largo", "30mm")),
                        InventoryVariantTemplate.of("6mm / 40mm", "DEM-TOR-6-40", 72, 12, money("0.18"), Map.of("diametro", "6mm", "largo", "40mm")),
                        InventoryVariantTemplate.of("8mm / 50mm", "DEM-TOR-8-50", 60, 10, money("0.24"), Map.of("diametro", "8mm", "largo", "50mm"))
                )
        ));
    }

    private static void addInventoryTemplates(
            List<InventoryTemplate> target,
            String categoryName,
            boolean sellable,
            List<String> productTypes,
            List<String> lines,
            BigDecimal basePrice
    ) {
        for (int typeIndex = 0; typeIndex < productTypes.size(); typeIndex++) {
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                String name = productTypes.get(typeIndex) + " " + lines.get(lineIndex);
                BigDecimal price = basePrice
                        .add(BigDecimal.valueOf(typeIndex).multiply(new BigDecimal("0.45")))
                        .add(BigDecimal.valueOf(lineIndex).multiply(new BigDecimal("0.30")))
                        .setScale(2, RoundingMode.HALF_UP);
                if (!containsInventoryTemplateName(target, name)) {
                    target.add(InventoryTemplate.generated(name, categoryName, price, sellable));
                }
            }
        }
    }

    private static boolean containsInventoryTemplateName(List<InventoryTemplate> templates, String name) {
        return templates.stream().anyMatch(template -> template.name().equalsIgnoreCase(name));
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

    private record ProfessionalSeed(
            String fullName,
            String specialty,
            String phone,
            String email,
            boolean active,
            List<String> preferredCategories
    ) {
    }

    private record SeededProfessional(ProfessionalResponse response, ProfessionalSeed seed) {
    }

    private record InventoryTemplate(
            String name,
            String categoryName,
            BigDecimal purchasePrice,
            boolean sellable,
            Integer stock,
            Integer minimumStock,
            String sku,
            List<InventoryVariantTemplate> variants
    ) {

        static InventoryTemplate generated(String name, String categoryName, BigDecimal purchasePrice, boolean sellable) {
            return new InventoryTemplate(name, categoryName, purchasePrice, sellable, null, null, null, List.of());
        }

        static InventoryTemplate simple(
                String name,
                String categoryName,
                BigDecimal purchasePrice,
                boolean sellable,
                int stock,
                int minimumStock,
                String sku
        ) {
            return new InventoryTemplate(name, categoryName, purchasePrice, sellable, stock, minimumStock, sku, List.of());
        }

        static InventoryTemplate withVariants(
                String name,
                String categoryName,
                boolean sellable,
                List<InventoryVariantTemplate> variants
        ) {
            return new InventoryTemplate(name, categoryName, null, sellable, 0, 0, null, variants);
        }

        boolean hasVariants() {
            return !variants.isEmpty();
        }
    }

    private record InventoryVariantTemplate(
            String variantLabel,
            String sku,
            int stock,
            int minimumStock,
            BigDecimal purchasePrice,
            Map<String, String> attributes
    ) {

        static InventoryVariantTemplate of(
                String variantLabel,
                String sku,
                int stock,
                int minimumStock,
                BigDecimal purchasePrice,
                Map<String, String> attributes
        ) {
            return new InventoryVariantTemplate(variantLabel, sku, stock, minimumStock, purchasePrice, attributes);
        }
    }

    private record SeededInventoryProduct(
            Long id,
            String name,
            BigDecimal purchasePrice,
            boolean sellable,
            StockProfile stockProfile,
            String categoryName,
            boolean hasVariants,
            List<SeededInventoryVariant> variants
    ) {
    }

    private record SeededInventoryVariant(
            Long id,
            String variantLabel,
            String sku,
            BigDecimal purchasePrice,
            int initialStock,
            StockProfile stockProfile
    ) {
    }

    private record SeededInventoryItem(
            Long productId,
            Long variantId,
            String productName,
            String itemName,
            BigDecimal purchasePrice,
            boolean sellable,
            StockProfile stockProfile,
            String categoryName
    ) {
        boolean variant() {
            return variantId != null;
        }

        InventoryItemKey key() {
            return new InventoryItemKey(productId, variantId);
        }
    }

    private record InventoryItemKey(Long productId, Long variantId) {
    }

    private record StockSnapshot(int stock, int minimumStock) {
    }

    private record RecurringExpenseTemplate(
            String concept,
            ExpenseCategory category,
            int quantity,
            BigDecimal baseAmount,
            int dayOfMonth
    ) {
    }

    private record StrategicExpensePlan(
            YearMonth month,
            String concept,
            ExpenseCategory category,
            int quantity,
            BigDecimal amount,
            int dayOfMonth,
            String notes
    ) {
    }

    private record MonthPlan(
            YearMonth month,
            int additionalAppointments,
            int confirmedPercent,
            int cancelledPercent,
            int walkInPercent,
            int productSalesPerWeek,
            int manualServiceSales,
            BigDecimal appointmentPriceFactor,
            BigDecimal manualServicePriceFactor,
            BigDecimal recurringExpenseMultiplier,
            OutcomeTarget targetOutcome,
            BigDecimal targetMargin
    ) {
    }

    private enum StockProfile {
        OUT,
        LOW,
        BALANCED,
        HIGH
    }

    private enum OutcomeTarget {
        GAIN,
        LOSS,
        FLEX
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
        private int inventoryVariantsCreated;
        private int automaticInventoryExpensesCreated;
        private int manualExpensesCreated;
        private int appointmentsCreated;
        private int automaticAppointmentSalesCreated;
        private int manualProductSalesCreated;
        private int manualServiceSalesCreated;
        private int correctiveExpensesCreated;
        private int correctiveServiceSalesCreated;
        private int daysCoveredWithAppointments;
        private int weeksCoveredWithProductSales;
        private long totalCategories;
        private long totalServices;
        private long totalClients;
        private long totalProfessionals;
        private long totalInventoryProducts;
        private long totalExpenses;
        private long totalAppointments;
        private long totalSales;

        private String toLogMessage() {
            return "categorias=" + totalCategories
                    + ", servicios=" + totalServices
                    + ", clientes=" + totalClients
                    + ", profesionales=" + totalProfessionals
                    + ", inventario=" + totalInventoryProducts
                    + ", variantesInventario=" + inventoryVariantsCreated
                    + ", gastosTotales=" + totalExpenses
                    + ", citasTotales=" + totalAppointments
                    + ", ventasTotales=" + totalSales
                    + ", ventasProductoManuales=" + manualProductSalesCreated
                    + ", ventasServicioManuales=" + manualServiceSalesCreated
                    + ", ventasAutomaticasPorCita=" + automaticAppointmentSalesCreated
                    + ", coberturasDias=" + daysCoveredWithAppointments
                    + ", coberturasSemanasProducto=" + weeksCoveredWithProductSales;
        }
    }
}
