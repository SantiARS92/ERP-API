package es.pedro.pymes_backend.infrastructure.repository;

import es.pedro.pymes_backend.domain.AppointmentEntity;
import es.pedro.pymes_backend.domain.AppointmentStatus;
import es.pedro.pymes_backend.domain.CategoryEntity;
import es.pedro.pymes_backend.domain.ClientEntity;
import es.pedro.pymes_backend.domain.ExpenseCategory;
import es.pedro.pymes_backend.domain.ExpenseEntity;
import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.domain.SaleEntity;
import es.pedro.pymes_backend.domain.SaleItemType;
import es.pedro.pymes_backend.domain.SaleSourceType;
import es.pedro.pymes_backend.domain.ServiceEntity;
import es.pedro.pymes_backend.infrastructure.repository.report.DayOfWeekAppointmentCountReportRow;
import es.pedro.pymes_backend.infrastructure.repository.report.MonthlyAmountReportRow;
import es.pedro.pymes_backend.infrastructure.repository.report.MonthlyCountReportRow;
import es.pedro.pymes_backend.infrastructure.repository.report.MonthlyOperationalSummaryReportRow;
import es.pedro.pymes_backend.infrastructure.repository.report.ProductMonthlyRankingReportRow;
import es.pedro.pymes_backend.infrastructure.repository.report.ReportPeriodTotals;
import es.pedro.pymes_backend.infrastructure.repository.report.ServiceMonthlyPerformanceReportRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportQueryRepositoryIntegrationTest {

    @Autowired
    private ReportQueryRepository reportQueryRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private InventoryProductRepository inventoryProductRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Test
    void shouldBuildReportAggregatesFromExistingEntities() {
        CategoryEntity serviceCategory = saveCategory("Servicios");
        CategoryEntity productCategory = saveCategory("Productos");

        ClientEntity client = saveClient("Cliente reportes");

        ServiceEntity haircut = saveService("Corte", 60, 30.0, serviceCategory);
        ServiceEntity color = saveService("Color", 90, 40.0, serviceCategory);

        InventoryProductEntity shampoo = saveProduct("Shampoo", 50, productCategory);
        InventoryProductEntity conditioner = saveProduct("Acondicionador", 50, productCategory);

        AppointmentEntity haircutMarchFirst = saveAppointment(client, haircut, LocalDate.of(2026, 3, 3), LocalTime.of(10, 0), AppointmentStatus.CONFIRMED);
        AppointmentEntity haircutMarchSecond = saveAppointment(client, haircut, LocalDate.of(2026, 3, 10), LocalTime.of(10, 0), AppointmentStatus.CONFIRMED);
        AppointmentEntity colorMarch = saveAppointment(client, color, LocalDate.of(2026, 3, 15), LocalTime.of(11, 0), AppointmentStatus.CONFIRMED);
        AppointmentEntity haircutApril = saveAppointment(client, haircut, LocalDate.of(2026, 4, 2), LocalTime.of(9, 30), AppointmentStatus.CONFIRMED);
        saveAppointment(client, haircut, LocalDate.of(2026, 3, 18), LocalTime.of(12, 0), AppointmentStatus.PENDING);

        saveAppointmentSale(client, haircut, haircutMarchFirst, new BigDecimal("30.00"));
        saveAppointmentSale(client, haircut, haircutMarchSecond, new BigDecimal("35.00"));
        saveAppointmentSale(client, color, colorMarch, new BigDecimal("40.00"));
        saveAppointmentSale(client, haircut, haircutApril, new BigDecimal("30.00"));

        saveProductSale(client, shampoo, 3, new BigDecimal("9.00"), LocalDate.of(2026, 3, 5));
        saveProductSale(client, conditioner, 2, new BigDecimal("10.00"), LocalDate.of(2026, 3, 20));
        saveProductSale(client, shampoo, 1, new BigDecimal("9.00"), LocalDate.of(2026, 4, 5));

        saveExpense("Material marzo", LocalDate.of(2026, 3, 2), new BigDecimal("15.00"));
        saveExpense("Proveedor marzo", LocalDate.of(2026, 3, 20), new BigDecimal("10.00"));
        saveExpense("Material abril", LocalDate.of(2026, 4, 10), new BigDecimal("8.00"));

        ReportPeriodTotals periodTotals = reportQueryRepository.findPeriodTotals(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        );

        assertThat(periodTotals.totalSales()).isEqualByComparingTo("152.00");
        assertThat(periodTotals.totalExpenses()).isEqualByComparingTo("25.00");
        assertThat(periodTotals.confirmedAppointments()).isEqualTo(3L);

        Map<YearMonth, BigDecimal> incomeByMonth = toAmountMap(
                reportQueryRepository.findMonthlyIncomeEvolution(YearMonth.of(2026, 3), YearMonth.of(2026, 4))
        );
        assertThat(incomeByMonth.get(YearMonth.of(2026, 3))).isEqualByComparingTo("152.00");
        assertThat(incomeByMonth.get(YearMonth.of(2026, 4))).isEqualByComparingTo("39.00");

        Map<YearMonth, BigDecimal> expenseByMonth = toAmountMap(
                reportQueryRepository.findMonthlyExpenseEvolution(YearMonth.of(2026, 3), YearMonth.of(2026, 4))
        );
        assertThat(expenseByMonth.get(YearMonth.of(2026, 3))).isEqualByComparingTo("25.00");
        assertThat(expenseByMonth.get(YearMonth.of(2026, 4))).isEqualByComparingTo("8.00");

        Map<YearMonth, Long> appointmentsByMonth = toCountMap(
                reportQueryRepository.findMonthlyConfirmedAppointmentsEvolution(YearMonth.of(2026, 3), YearMonth.of(2026, 4))
        );
        assertThat(appointmentsByMonth).containsEntry(YearMonth.of(2026, 3), 3L);
        assertThat(appointmentsByMonth).containsEntry(YearMonth.of(2026, 4), 1L);

        Map<DayOfWeek, Long> occupiedDays = toDayOfWeekMap(
                reportQueryRepository.countConfirmedAppointmentsByDayOfWeek(YearMonth.of(2026, 3))
        );
        assertThat(occupiedDays).containsEntry(DayOfWeek.TUESDAY, 2L);
        assertThat(occupiedDays).containsEntry(DayOfWeek.SUNDAY, 1L);
        assertThat(occupiedDays).containsEntry(DayOfWeek.MONDAY, 0L);

        assertThat(reportQueryRepository.findTopServicesOfMonth(YearMonth.of(2026, 3)))
                .containsExactly(
                        new ServiceMonthlyPerformanceReportRow(
                                haircut.getId(),
                                haircut.getName(),
                                serviceCategory.getName(),
                                haircut.getDurationMinutes(),
                                2L,
                                new BigDecimal("65.00")
                        ),
                        new ServiceMonthlyPerformanceReportRow(
                                color.getId(),
                                color.getName(),
                                serviceCategory.getName(),
                                color.getDurationMinutes(),
                                1L,
                                new BigDecimal("40.00")
                        )
                );

        assertThat(reportQueryRepository.findProductRankingOfMonth(YearMonth.of(2026, 3)))
                .containsExactly(
                        new ProductMonthlyRankingReportRow(
                                shampoo.getId(),
                                shampoo.getName(),
                                productCategory.getName(),
                                3L,
                                new BigDecimal("27.00")
                        ),
                        new ProductMonthlyRankingReportRow(
                                conditioner.getId(),
                                conditioner.getName(),
                                productCategory.getName(),
                                2L,
                                new BigDecimal("20.00")
                        )
                );

        assertThat(reportQueryRepository.findMonthlyOperationalSummary(YearMonth.of(2026, 3), YearMonth.of(2026, 4)))
                .containsExactly(
                        new MonthlyOperationalSummaryReportRow(
                                YearMonth.of(2026, 3),
                                new BigDecimal("152.00"),
                                new BigDecimal("25.00"),
                                new BigDecimal("127.00")
                        ),
                        new MonthlyOperationalSummaryReportRow(
                                YearMonth.of(2026, 4),
                                new BigDecimal("39.00"),
                                new BigDecimal("8.00"),
                                new BigDecimal("31.00")
                        )
                );
    }

    private Map<YearMonth, BigDecimal> toAmountMap(Iterable<MonthlyAmountReportRow> rows) {
        Map<YearMonth, BigDecimal> values = new LinkedHashMap<>();
        for (MonthlyAmountReportRow row : rows) {
            values.put(row.month(), row.amount());
        }
        return values;
    }

    private Map<YearMonth, Long> toCountMap(Iterable<MonthlyCountReportRow> rows) {
        Map<YearMonth, Long> values = new LinkedHashMap<>();
        for (MonthlyCountReportRow row : rows) {
            values.put(row.month(), row.count());
        }
        return values;
    }

    private Map<DayOfWeek, Long> toDayOfWeekMap(Iterable<DayOfWeekAppointmentCountReportRow> rows) {
        Map<DayOfWeek, Long> values = new LinkedHashMap<>();
        for (DayOfWeekAppointmentCountReportRow row : rows) {
            values.put(row.dayOfWeek(), row.confirmedAppointments());
        }
        return values;
    }

    private CategoryEntity saveCategory(String namePrefix) {
        return categoryRepository.saveAndFlush(
                CategoryEntity.builder()
                        .name(namePrefix + "-" + UUID.randomUUID())
                        .color("blue")
                        .icon("tag")
                        .build()
        );
    }

    private ClientEntity saveClient(String fullName) {
        ClientEntity client = new ClientEntity();
        client.setFullName(fullName);
        client.setPhone("600000000");
        client.setEmail(fullName.replace(" ", ".").toLowerCase() + "@test.com");
        return clientRepository.saveAndFlush(client);
    }

    private ServiceEntity saveService(String name, int durationMinutes, double price, CategoryEntity category) {
        return serviceRepository.saveAndFlush(
                ServiceEntity.builder()
                        .name(name)
                        .description("Servicio para reportes")
                        .durationMinutes(durationMinutes)
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
                        .description("Producto para reportes")
                        .category(category)
                        .stock(stock)
                        .minimumStock(2)
                        .sku("SKU-" + UUID.randomUUID())
                        .purchasePrice(new BigDecimal("4.00"))
                        .active(true)
                        .build()
        );
    }

    private AppointmentEntity saveAppointment(
            ClientEntity client,
            ServiceEntity service,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            AppointmentStatus status
    ) {
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setClientId(client.getId());
        appointment.setClientName(client.getFullName());
        appointment.setServiceId(service.getId());
        appointment.setServiceName(service.getName());
        appointment.setPrice(service.getPrice());
        appointment.setAppointmentDate(appointmentDate);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setStatus(status);
        appointment.setInternalNotes("Reporte");
        return appointmentRepository.saveAndFlush(appointment);
    }

    private SaleEntity saveAppointmentSale(
            ClientEntity client,
            ServiceEntity service,
            AppointmentEntity appointment,
            BigDecimal totalAmount
    ) {
        SaleEntity sale = new SaleEntity();
        sale.setSourceType(SaleSourceType.APPOINTMENT);
        sale.setAppointmentId(appointment.getId());
        sale.setClient(client);
        sale.setClientName(client.getFullName());
        sale.setItemType(SaleItemType.SERVICE);
        sale.setService(service);
        sale.setInventoryProduct(null);
        sale.setItemName(service.getName());
        sale.setUnitPrice(totalAmount);
        sale.setQuantity(1);
        sale.setTotalAmount(totalAmount);
        sale.setSaleDate(appointment.getAppointmentDate());
        sale.setNotes("Venta desde cita");
        return saleRepository.saveAndFlush(sale);
    }

    private SaleEntity saveProductSale(
            ClientEntity client,
            InventoryProductEntity product,
            int quantity,
            BigDecimal unitPrice,
            LocalDate saleDate
    ) {
        SaleEntity sale = new SaleEntity();
        sale.setSourceType(SaleSourceType.MANUAL);
        sale.setAppointmentId(null);
        sale.setClient(client);
        sale.setClientName(client.getFullName());
        sale.setItemType(SaleItemType.PRODUCT);
        sale.setService(null);
        sale.setInventoryProduct(product);
        sale.setItemName(product.getName());
        sale.setUnitPrice(unitPrice);
        sale.setQuantity(quantity);
        sale.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        sale.setSaleDate(saleDate);
        sale.setNotes("Venta de producto");
        return saleRepository.saveAndFlush(sale);
    }

    private ExpenseEntity saveExpense(String concept, LocalDate expenseDate, BigDecimal amount) {
        return expenseRepository.saveAndFlush(
                ExpenseEntity.builder()
                        .expenseDate(expenseDate)
                        .concept(concept)
                        .category(ExpenseCategory.SUPPLIES)
                        .quantity(1)
                        .amount(amount)
                        .notes("Gasto test")
                        .stockMovementApplied(false)
                        .build()
        );
    }
}
