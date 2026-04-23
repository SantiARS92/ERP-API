package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.AppointmentEntity;
import es.pedro.pymes_backend.domain.AppointmentStatus;
import es.pedro.pymes_backend.domain.ExpenseCategory;
import es.pedro.pymes_backend.domain.ExpenseEntity;
import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.domain.InventoryProductVariantEntity;
import es.pedro.pymes_backend.domain.SaleEntity;
import es.pedro.pymes_backend.domain.SaleItemType;
import es.pedro.pymes_backend.domain.SaleSourceType;
import es.pedro.pymes_backend.infrastructure.repository.AppointmentRepository;
import es.pedro.pymes_backend.infrastructure.repository.CategoryRepository;
import es.pedro.pymes_backend.infrastructure.repository.ClientRepository;
import es.pedro.pymes_backend.infrastructure.repository.ExpenseRepository;
import es.pedro.pymes_backend.infrastructure.repository.InventoryProductRepository;
import es.pedro.pymes_backend.infrastructure.repository.InventoryProductVariantRepository;
import es.pedro.pymes_backend.infrastructure.repository.ProfessionalRepository;
import es.pedro.pymes_backend.infrastructure.repository.SaleRepository;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.seed.demo-data=true")
@ActiveProfiles("test")
class DevDataSeederIntegrationTest {

    private static final LocalDate START_DATE = LocalDate.of(2024, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2027, 12, 31);

    @Autowired
    private DevDataSeeder devDataSeeder;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private InventoryProductRepository inventoryProductRepository;

    @Autowired
    private InventoryProductVariantRepository inventoryProductVariantRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Test
    void shouldSeedExtendedDemoDataOnceAndSkipRepeatedExecution() {
        List<AppointmentEntity> appointments = appointmentRepository.findAll();
        List<SaleEntity> sales = saleRepository.findAll();
        List<ExpenseEntity> expenses = expenseRepository.findAll();
        List<InventoryProductEntity> products = inventoryProductRepository.findAll();
        List<InventoryProductVariantEntity> variants = inventoryProductVariantRepository.findAll();

        assertThat(categoryRepository.count()).isEqualTo(10);
        assertThat(serviceRepository.count()).isEqualTo(40);
        assertThat(clientRepository.count()).isEqualTo(360);
        assertThat(professionalRepository.count()).isEqualTo(7);
        assertThat(inventoryProductRepository.count()).isEqualTo(160);
        assertThat(inventoryProductVariantRepository.count()).isGreaterThanOrEqualTo(8);
        assertThat(expenseRepository.count()).isGreaterThanOrEqualTo(500);
        assertThat(appointmentRepository.count()).isGreaterThanOrEqualTo(2000);
        assertThat(saleRepository.count()).isGreaterThanOrEqualTo(2400);

        assertThat(clientRepository.findByFullNameIgnoreCase("Al contado"))
                .isPresent()
                .get()
                .satisfies(client -> {
                    assertThat(client.getPhone()).isEmpty();
                    assertThat(client.getEmail()).isNull();
                    assertThat(client.getAcceptsInvoices()).isFalse();
                    assertThat(client.getAddress()).isNull();
                    assertThat(client.getCifNif()).isNull();
                    assertThat(client.getIdentificador()).isNull();
                });

        assertThat(professionalRepository.findByActiveOrderByFullNameAsc(true)).hasSize(6);
        assertThat(appointments.stream().map(AppointmentEntity::getStatus))
                .contains(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED);
        assertThat(sales.stream().map(SaleEntity::getSourceType))
                .contains(SaleSourceType.MANUAL, SaleSourceType.APPOINTMENT);

        assertDailyAppointmentCoverage(appointments);
        assertWeeklyProductSalesCoverage(sales);
        assertEconomicDiversity(sales, expenses);
        assertInventoryDiversity(products);
        assertVariantInventoryDemoData(products, variants);
        assertVariantSalesAndExpenses(sales, expenses);

        long categoryCount = categoryRepository.count();
        long serviceCount = serviceRepository.count();
        long clientCount = clientRepository.count();
        long professionalCount = professionalRepository.count();
        long inventoryCount = inventoryProductRepository.count();
        long variantCount = inventoryProductVariantRepository.count();
        long expenseCount = expenseRepository.count();
        long appointmentCount = appointmentRepository.count();
        long saleCount = saleRepository.count();

        devDataSeeder.run(new DefaultApplicationArguments(new String[0]));

        assertThat(categoryRepository.count()).isEqualTo(categoryCount);
        assertThat(serviceRepository.count()).isEqualTo(serviceCount);
        assertThat(clientRepository.count()).isEqualTo(clientCount);
        assertThat(professionalRepository.count()).isEqualTo(professionalCount);
        assertThat(inventoryProductRepository.count()).isEqualTo(inventoryCount);
        assertThat(inventoryProductVariantRepository.count()).isEqualTo(variantCount);
        assertThat(expenseRepository.count()).isEqualTo(expenseCount);
        assertThat(appointmentRepository.count()).isEqualTo(appointmentCount);
        assertThat(saleRepository.count()).isEqualTo(saleCount);
    }

    private void assertDailyAppointmentCoverage(List<AppointmentEntity> appointments) {
        Map<LocalDate, Long> appointmentsByDate = appointments.stream()
                .collect(Collectors.groupingBy(AppointmentEntity::getAppointmentDate, Collectors.counting()));

        LocalDate date = START_DATE;
        while (!date.isAfter(END_DATE)) {
            assertThat(appointmentsByDate.getOrDefault(date, 0L))
                    .withFailMessage("Falta cobertura de citas para %s", date)
                    .isGreaterThanOrEqualTo(1L);
            date = date.plusDays(1);
        }
    }

    private void assertWeeklyProductSalesCoverage(List<SaleEntity> sales) {
        Map<LocalDate, Long> productSalesByWeek = sales.stream()
                .filter(sale -> sale.getItemType() == SaleItemType.PRODUCT)
                .collect(Collectors.groupingBy(
                        sale -> sale.getSaleDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                        Collectors.counting()
                ));

        LocalDate weekStart = START_DATE.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        while (!weekStart.isAfter(END_DATE)) {
            if (!weekStart.isBefore(START_DATE)) {
                assertThat(productSalesByWeek.getOrDefault(weekStart, 0L))
                        .withFailMessage("Faltan ventas de producto en la semana de %s", weekStart)
                        .isGreaterThanOrEqualTo(5L);
            }
            weekStart = weekStart.plusWeeks(1);
        }
    }

    private void assertEconomicDiversity(List<SaleEntity> sales, List<ExpenseEntity> expenses) {
        assertThat(profitBetween(sales, expenses, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)))
                .isGreaterThan(BigDecimal.ZERO);
        assertThat(profitBetween(sales, expenses, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)))
                .isLessThan(BigDecimal.ZERO);
        assertThat(profitBetween(sales, expenses, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31)))
                .isGreaterThan(BigDecimal.ZERO);

        Map<YearMonth, BigDecimal> monthlyProfit = buildMonthlyProfit(sales, expenses);
        assertThat(monthlyProfit.values()).anyMatch(value -> value.compareTo(BigDecimal.ZERO) > 0);
        assertThat(monthlyProfit.values()).anyMatch(value -> value.compareTo(BigDecimal.ZERO) < 0);

        Map<String, BigDecimal> quarterlyProfit = buildQuarterlyProfit(monthlyProfit);
        assertThat(quarterlyProfit.values()).anyMatch(value -> value.compareTo(BigDecimal.ZERO) > 0);
        assertThat(quarterlyProfit.values()).anyMatch(value -> value.compareTo(BigDecimal.ZERO) < 0);
    }

    private void assertInventoryDiversity(List<InventoryProductEntity> products) {
        assertThat(products).anyMatch(product -> product.getStock() == 0);
        assertThat(products).anyMatch(product -> product.getStock() > 0 && product.getStock() <= product.getMinimumStock());
        assertThat(products).anyMatch(product -> product.getStock() > product.getMinimumStock() + 8);
    }

    private void assertVariantInventoryDemoData(
            List<InventoryProductEntity> products,
            List<InventoryProductVariantEntity> variants
    ) {
        assertThat(products)
                .filteredOn(product -> !Boolean.TRUE.equals(product.getHasVariants()))
                .extracting(InventoryProductEntity::getName)
                .contains(
                        "Champu L'Oreal Prof",
                        "Acondicionador Kerastase",
                        "Laca Fijacion Fuerte",
                        "Crema Hidratante Facial",
                        "Aceite Esencial Lavanda"
                );

        assertVariantProduct(products, "Camiseta Basica", List.of("Roja / S", "Roja / M", "Azul / M", "Azul / L"));
        assertVariantProduct(products, "Tornillo Galvanizado", List.of("4mm / 20mm", "4mm / 30mm", "6mm / 40mm", "8mm / 50mm"));

        assertThat(variants)
                .filteredOn(variant -> Boolean.TRUE.equals(variant.getActive()))
                .hasSizeGreaterThanOrEqualTo(8)
                .allSatisfy(variant -> {
                    assertThat(variant.getSku()).isNotBlank();
                    assertThat(variant.getStock()).isNotNegative();
                    assertThat(variant.getMinimumStock()).isNotNegative();
                    assertThat(variant.getPurchasePrice()).isGreaterThan(BigDecimal.ZERO);
                });
    }

    private void assertVariantProduct(
            List<InventoryProductEntity> products,
            String productName,
            List<String> expectedLabels
    ) {
        InventoryProductEntity product = findProduct(products, productName);
        assertThat(product.getHasVariants()).isTrue();
        assertThat(product.getStock()).isZero();
        assertThat(product.getMinimumStock()).isZero();
        assertThat(product.getSku()).isNull();
        assertThat(product.getPurchasePrice()).isNull();

        List<InventoryProductVariantEntity> activeVariants =
                inventoryProductVariantRepository.findByProductIdAndActiveTrueOrderByVariantLabelAscIdAsc(product.getId());

        assertThat(activeVariants)
                .extracting(InventoryProductVariantEntity::getVariantLabel)
                .containsExactlyInAnyOrderElementsOf(expectedLabels);
        assertThat(activeVariants.stream().mapToInt(InventoryProductVariantEntity::getStock).sum()).isPositive();
    }

    private void assertVariantSalesAndExpenses(List<SaleEntity> sales, List<ExpenseEntity> expenses) {
        assertThat(sales).anyMatch(sale -> sale.getItemType() == SaleItemType.PRODUCT
                && sale.getInventoryProduct() != null
                && sale.getInventoryProductVariant() == null
                && sale.getItemName().equals("Champu L'Oreal Prof"));
        assertThat(sales).anyMatch(sale -> sale.getItemType() == SaleItemType.PRODUCT
                && sale.getInventoryProduct() != null
                && sale.getInventoryProductVariant() != null
                && sale.getItemName().equals("Camiseta Basica - Azul / M"));

        assertThat(expenses).anyMatch(expense -> expense.getCategory() == ExpenseCategory.SUPPLIES
                && expense.getInventoryProduct() != null
                && expense.getInventoryProductVariant() == null
                && expense.getConcept().equals("Champu L'Oreal Prof")
                && Boolean.TRUE.equals(expense.getStockMovementApplied()));
        assertThat(expenses).anyMatch(expense -> expense.getCategory() == ExpenseCategory.SUPPLIES
                && expense.getInventoryProduct() != null
                && expense.getInventoryProductVariant() != null
                && expense.getConcept().equals("Camiseta Basica - Roja / M")
                && Boolean.TRUE.equals(expense.getStockMovementApplied()));
        assertThat(expenses).anyMatch(expense -> expense.getCategory() == ExpenseCategory.SUPPLIES
                && expense.getInventoryProduct() != null
                && expense.getInventoryProductVariant() != null
                && expense.getConcept().equals("Tornillo Galvanizado - 4mm / 20mm")
                && Boolean.FALSE.equals(expense.getStockMovementApplied()));
    }

    private InventoryProductEntity findProduct(List<InventoryProductEntity> products, String productName) {
        return products.stream()
                .filter(product -> product.getName().equals(productName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No existe producto demo: " + productName));
    }

    private BigDecimal profitBetween(List<SaleEntity> sales, List<ExpenseEntity> expenses, LocalDate fromDate, LocalDate toDate) {
        BigDecimal totalSales = sales.stream()
                .filter(sale -> !sale.getSaleDate().isBefore(fromDate) && !sale.getSaleDate().isAfter(toDate))
                .map(SaleEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = expenses.stream()
                .filter(expense -> !expense.getExpenseDate().isBefore(fromDate) && !expense.getExpenseDate().isAfter(toDate))
                .map(ExpenseEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalSales.subtract(totalExpenses);
    }

    private Map<YearMonth, BigDecimal> buildMonthlyProfit(List<SaleEntity> sales, List<ExpenseEntity> expenses) {
        Map<YearMonth, BigDecimal> incomeByMonth = sales.stream()
                .collect(Collectors.groupingBy(
                        sale -> YearMonth.from(sale.getSaleDate()),
                        Collectors.mapping(SaleEntity::getTotalAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        Map<YearMonth, BigDecimal> expenseByMonth = expenses.stream()
                .collect(Collectors.groupingBy(
                        expense -> YearMonth.from(expense.getExpenseDate()),
                        Collectors.mapping(ExpenseEntity::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        Map<YearMonth, BigDecimal> result = new java.util.LinkedHashMap<>();
        YearMonth month = YearMonth.from(START_DATE);
        while (!month.isAfter(YearMonth.from(END_DATE))) {
            result.put(month, incomeByMonth.getOrDefault(month, BigDecimal.ZERO).subtract(expenseByMonth.getOrDefault(month, BigDecimal.ZERO)));
            month = month.plusMonths(1);
        }
        return result;
    }

    private Map<String, BigDecimal> buildQuarterlyProfit(Map<YearMonth, BigDecimal> monthlyProfit) {
        return monthlyProfit.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().getYear() + "-Q" + (((entry.getKey().getMonthValue() - 1) / 3) + 1),
                        Collectors.mapping(Map.Entry::getValue, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));
    }
}
