package es.pedro.pymes_backend.infrastructure.repository;

import es.pedro.pymes_backend.domain.AppointmentEntity;
import es.pedro.pymes_backend.domain.AppointmentStatus;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Repository
@Transactional(readOnly = true)
public class ReportQueryRepository {

    private static final Comparator<ServiceMonthlyPerformanceReportRow> SERVICE_RANKING_COMPARATOR =
            Comparator.comparingLong(ServiceMonthlyPerformanceReportRow::confirmedAppointments)
                    .reversed()
                    .thenComparing(ServiceMonthlyPerformanceReportRow::generatedIncome, Comparator.reverseOrder())
                    .thenComparing(row -> row.serviceName() == null ? "" : row.serviceName(), String.CASE_INSENSITIVE_ORDER);

    private static final Comparator<ProductMonthlyRankingReportRow> PRODUCT_RANKING_COMPARATOR =
            Comparator.comparingLong(ProductMonthlyRankingReportRow::unitsSold)
                    .reversed()
                    .thenComparing(ProductMonthlyRankingReportRow::generatedIncome, Comparator.reverseOrder())
                    .thenComparing(row -> row.productName() == null ? "" : row.productName(), String.CASE_INSENSITIVE_ORDER);

    private final EntityManager entityManager;

    public ReportQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public ReportPeriodTotals findPeriodTotals(LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate);

        BigDecimal totalSales = amountOrZero(entityManager.createQuery("""
                        select sum(s.totalAmount)
                        from SaleEntity s
                        where s.saleDate between :fromDate and :toDate
                        """, BigDecimal.class)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getSingleResult());

        BigDecimal totalExpenses = amountOrZero(entityManager.createQuery("""
                        select sum(e.amount)
                        from ExpenseEntity e
                        where e.expenseDate between :fromDate and :toDate
                        """, BigDecimal.class)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getSingleResult());

        long confirmedAppointments = entityManager.createQuery("""
                        select count(a)
                        from AppointmentEntity a
                        where a.status = :status
                          and a.appointmentDate between :fromDate and :toDate
                        """, Long.class)
                .setParameter("status", AppointmentStatus.CONFIRMED)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getSingleResult();

        return new ReportPeriodTotals(totalSales, totalExpenses, confirmedAppointments);
    }

    public List<MonthlyAmountReportRow> findMonthlyIncomeEvolution(YearMonth fromMonth, YearMonth toMonth) {
        return toMonthlyAmountRows(aggregateSalesByMonth(fromMonth, toMonth));
    }

    public List<MonthlyAmountReportRow> findMonthlyExpenseEvolution(YearMonth fromMonth, YearMonth toMonth) {
        return toMonthlyAmountRows(aggregateExpensesByMonth(fromMonth, toMonth));
    }

    public List<MonthlyCountReportRow> findMonthlyConfirmedAppointmentsEvolution(YearMonth fromMonth, YearMonth toMonth) {
        return toMonthlyCountRows(aggregateConfirmedAppointmentsByMonth(fromMonth, toMonth));
    }

    public List<DayOfWeekAppointmentCountReportRow> countConfirmedAppointmentsByDayOfWeek(YearMonth month) {
        Objects.requireNonNull(month, "month is required");

        EnumMap<DayOfWeek, Long> totalsByDay = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            totalsByDay.put(dayOfWeek, 0L);
        }

        DateRange dateRange = monthRange(month, month);
        for (LocalDate appointmentDate : findConfirmedAppointmentDatesBetween(dateRange.fromDate(), dateRange.toDate())) {
            totalsByDay.merge(appointmentDate.getDayOfWeek(), 1L, Long::sum);
        }

        List<DayOfWeekAppointmentCountReportRow> rows = new ArrayList<>(DayOfWeek.values().length);
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            rows.add(new DayOfWeekAppointmentCountReportRow(dayOfWeek, totalsByDay.get(dayOfWeek)));
        }
        return rows;
    }

    public List<ServiceMonthlyPerformanceReportRow> findTopServicesOfMonth(YearMonth month) {
        Objects.requireNonNull(month, "month is required");

        DateRange dateRange = monthRange(month, month);
        List<AppointmentEntity> confirmedAppointments = findConfirmedAppointmentsBetween(dateRange.fromDate(), dateRange.toDate());
        if (confirmedAppointments.isEmpty()) {
            return List.of();
        }

        Map<Long, ServiceAccumulator> servicesById = new LinkedHashMap<>();
        for (AppointmentEntity appointment : confirmedAppointments) {
            if (appointment.getServiceId() == null) {
                continue;
            }

            ServiceAccumulator accumulator = servicesById.computeIfAbsent(
                    appointment.getServiceId(),
                    ignored -> new ServiceAccumulator(appointment.getServiceId(), appointment.getServiceName())
            );
            accumulator.registerAppointment(appointment.getServiceName());
        }

        if (servicesById.isEmpty()) {
            return List.of();
        }

        // AppointmentEntity only stores serviceId/serviceName, so metadata is resolved separately by id.
        Map<Long, ServiceMetadata> metadataByServiceId = loadServiceMetadata(servicesById.keySet());
        Map<Long, BigDecimal> incomeByServiceId = loadAppointmentServiceIncome(servicesById.keySet(), dateRange.fromDate(), dateRange.toDate());

        return servicesById.values().stream()
                .map(accumulator -> {
                    ServiceMetadata metadata = metadataByServiceId.get(accumulator.serviceId);
                    String serviceName = metadata != null ? metadata.serviceName : accumulator.snapshotName;
                    String categoryName = metadata != null ? metadata.categoryName : null;
                    Integer durationMinutes = metadata != null ? metadata.durationMinutes : null;
                    BigDecimal generatedIncome = incomeByServiceId.getOrDefault(accumulator.serviceId, BigDecimal.ZERO);

                    return new ServiceMonthlyPerformanceReportRow(
                            accumulator.serviceId,
                            serviceName,
                            categoryName,
                            durationMinutes,
                            accumulator.confirmedAppointments,
                            generatedIncome
                    );
                })
                .sorted(SERVICE_RANKING_COMPARATOR)
                .toList();
    }

    public List<ProductMonthlyRankingReportRow> findProductRankingOfMonth(YearMonth month) {
        Objects.requireNonNull(month, "month is required");

        DateRange dateRange = monthRange(month, month);
        List<SaleEntity> productSales = findProductSalesBetween(dateRange.fromDate(), dateRange.toDate());
        if (productSales.isEmpty()) {
            return List.of();
        }

        Map<Long, ProductAccumulator> productsById = new LinkedHashMap<>();
        for (SaleEntity sale : productSales) {
            if (sale.getInventoryProduct() == null || sale.getInventoryProduct().getId() == null) {
                continue;
            }

            Long productId = sale.getInventoryProduct().getId();
            ProductAccumulator accumulator = productsById.computeIfAbsent(
                    productId,
                    ignored -> new ProductAccumulator(productId, sale.getItemName())
            );
            accumulator.registerSale(sale.getItemName(), sale.getQuantity(), sale.getTotalAmount());
        }

        if (productsById.isEmpty()) {
            return List.of();
        }

        Map<Long, ProductMetadata> metadataByProductId = loadProductMetadata(productsById.keySet());

        return productsById.values().stream()
                .map(accumulator -> {
                    ProductMetadata metadata = metadataByProductId.get(accumulator.productId);
                    String productName = metadata != null ? metadata.productName : accumulator.snapshotName;
                    String categoryName = metadata != null ? metadata.categoryName : null;

                    return new ProductMonthlyRankingReportRow(
                            accumulator.productId,
                            productName,
                            categoryName,
                            accumulator.unitsSold,
                            accumulator.generatedIncome
                    );
                })
                .sorted(PRODUCT_RANKING_COMPARATOR)
                .toList();
    }

    public List<MonthlyOperationalSummaryReportRow> findMonthlyOperationalSummary(YearMonth fromMonth, YearMonth toMonth) {
        LinkedHashMap<YearMonth, BigDecimal> incomeByMonth = aggregateSalesByMonth(fromMonth, toMonth);
        LinkedHashMap<YearMonth, BigDecimal> expensesByMonth = aggregateExpensesByMonth(fromMonth, toMonth);

        List<MonthlyOperationalSummaryReportRow> rows = new ArrayList<>(incomeByMonth.size());
        for (Map.Entry<YearMonth, BigDecimal> entry : incomeByMonth.entrySet()) {
            YearMonth month = entry.getKey();
            BigDecimal income = entry.getValue();
            BigDecimal expenses = expensesByMonth.getOrDefault(month, BigDecimal.ZERO);

            rows.add(new MonthlyOperationalSummaryReportRow(
                    month,
                    income,
                    expenses,
                    income.subtract(expenses)
            ));
        }
        return rows;
    }

    private LinkedHashMap<YearMonth, BigDecimal> aggregateSalesByMonth(YearMonth fromMonth, YearMonth toMonth) {
        DateRange dateRange = monthRange(fromMonth, toMonth);
        LinkedHashMap<YearMonth, BigDecimal> totals = initializeAmountBuckets(fromMonth, toMonth);

        for (Tuple row : findSaleAmountRowsBetween(dateRange.fromDate(), dateRange.toDate())) {
            LocalDate businessDate = row.get("businessDate", LocalDate.class);
            BigDecimal amount = row.get("amount", BigDecimal.class);
            totals.merge(YearMonth.from(businessDate), amountOrZero(amount), BigDecimal::add);
        }

        return totals;
    }

    private LinkedHashMap<YearMonth, BigDecimal> aggregateExpensesByMonth(YearMonth fromMonth, YearMonth toMonth) {
        DateRange dateRange = monthRange(fromMonth, toMonth);
        LinkedHashMap<YearMonth, BigDecimal> totals = initializeAmountBuckets(fromMonth, toMonth);

        for (Tuple row : findExpenseAmountRowsBetween(dateRange.fromDate(), dateRange.toDate())) {
            LocalDate businessDate = row.get("businessDate", LocalDate.class);
            BigDecimal amount = row.get("amount", BigDecimal.class);
            totals.merge(YearMonth.from(businessDate), amountOrZero(amount), BigDecimal::add);
        }

        return totals;
    }

    private LinkedHashMap<YearMonth, Long> aggregateConfirmedAppointmentsByMonth(YearMonth fromMonth, YearMonth toMonth) {
        DateRange dateRange = monthRange(fromMonth, toMonth);
        LinkedHashMap<YearMonth, Long> totals = initializeCountBuckets(fromMonth, toMonth);

        for (LocalDate appointmentDate : findConfirmedAppointmentDatesBetween(dateRange.fromDate(), dateRange.toDate())) {
            totals.merge(YearMonth.from(appointmentDate), 1L, Long::sum);
        }

        return totals;
    }

    private List<Tuple> findSaleAmountRowsBetween(LocalDate fromDate, LocalDate toDate) {
        return entityManager.createQuery("""
                        select s.saleDate as businessDate, s.totalAmount as amount
                        from SaleEntity s
                        where s.saleDate between :fromDate and :toDate
                        order by s.saleDate asc
                        """, Tuple.class)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();
    }

    private List<Tuple> findExpenseAmountRowsBetween(LocalDate fromDate, LocalDate toDate) {
        return entityManager.createQuery("""
                        select e.expenseDate as businessDate, e.amount as amount
                        from ExpenseEntity e
                        where e.expenseDate between :fromDate and :toDate
                        order by e.expenseDate asc
                        """, Tuple.class)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();
    }

    private List<AppointmentEntity> findConfirmedAppointmentsBetween(LocalDate fromDate, LocalDate toDate) {
        return entityManager.createQuery("""
                        select a
                        from AppointmentEntity a
                        where a.status = :status
                          and a.appointmentDate between :fromDate and :toDate
                        order by a.appointmentDate asc, a.appointmentTime asc
                        """, AppointmentEntity.class)
                .setParameter("status", AppointmentStatus.CONFIRMED)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();
    }

    private List<LocalDate> findConfirmedAppointmentDatesBetween(LocalDate fromDate, LocalDate toDate) {
        return entityManager.createQuery("""
                        select a.appointmentDate
                        from AppointmentEntity a
                        where a.status = :status
                          and a.appointmentDate between :fromDate and :toDate
                        order by a.appointmentDate asc, a.appointmentTime asc
                        """, LocalDate.class)
                .setParameter("status", AppointmentStatus.CONFIRMED)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();
    }

    private List<SaleEntity> findProductSalesBetween(LocalDate fromDate, LocalDate toDate) {
        return entityManager.createQuery("""
                        select s
                        from SaleEntity s
                        where s.itemType = :productType
                          and s.inventoryProduct is not null
                          and s.saleDate between :fromDate and :toDate
                        order by s.saleDate asc, s.saleTime asc
                        """, SaleEntity.class)
                .setParameter("productType", SaleItemType.PRODUCT)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();
    }

    private Map<Long, BigDecimal> loadAppointmentServiceIncome(Set<Long> serviceIds, LocalDate fromDate, LocalDate toDate) {
        if (serviceIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, BigDecimal> incomeByServiceId = new LinkedHashMap<>();
        List<Tuple> rows = entityManager.createQuery("""
                        select s.service.id as serviceId, sum(s.totalAmount) as amount
                        from SaleEntity s
                        where s.itemType = :serviceType
                          and s.sourceType = :sourceType
                          and s.service is not null
                          and s.service.id in :serviceIds
                          and s.saleDate between :fromDate and :toDate
                        group by s.service.id
                        """, Tuple.class)
                .setParameter("serviceType", SaleItemType.SERVICE)
                .setParameter("sourceType", SaleSourceType.APPOINTMENT)
                .setParameter("serviceIds", serviceIds)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();

        for (Tuple row : rows) {
            Long serviceId = row.get("serviceId", Long.class);
            BigDecimal amount = row.get("amount", BigDecimal.class);
            if (serviceId != null) {
                incomeByServiceId.put(serviceId, amountOrZero(amount));
            }
        }

        return incomeByServiceId;
    }

    private Map<Long, ServiceMetadata> loadServiceMetadata(Set<Long> serviceIds) {
        if (serviceIds.isEmpty()) {
            return Map.of();
        }

        List<ServiceEntity> services = entityManager.createQuery("""
                        select distinct s
                        from ServiceEntity s
                        left join fetch s.category
                        where s.id in :serviceIds
                        """, ServiceEntity.class)
                .setParameter("serviceIds", serviceIds)
                .getResultList();

        Map<Long, ServiceMetadata> metadataByServiceId = new LinkedHashMap<>();
        for (ServiceEntity service : services) {
            metadataByServiceId.put(service.getId(), new ServiceMetadata(
                    service.getName(),
                    service.getCategory() != null ? service.getCategory().getName() : null,
                    service.getDurationMinutes()
            ));
        }
        return metadataByServiceId;
    }

    private Map<Long, ProductMetadata> loadProductMetadata(Set<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        List<InventoryProductEntity> products = entityManager.createQuery("""
                        select distinct p
                        from InventoryProductEntity p
                        left join fetch p.category
                        where p.id in :productIds
                        """, InventoryProductEntity.class)
                .setParameter("productIds", productIds)
                .getResultList();

        Map<Long, ProductMetadata> metadataByProductId = new LinkedHashMap<>();
        for (InventoryProductEntity product : products) {
            metadataByProductId.put(product.getId(), new ProductMetadata(
                    product.getName(),
                    product.getCategory() != null ? product.getCategory().getName() : null
            ));
        }
        return metadataByProductId;
    }

    private List<MonthlyAmountReportRow> toMonthlyAmountRows(LinkedHashMap<YearMonth, BigDecimal> totalsByMonth) {
        return totalsByMonth.entrySet().stream()
                .map(entry -> new MonthlyAmountReportRow(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<MonthlyCountReportRow> toMonthlyCountRows(LinkedHashMap<YearMonth, Long> totalsByMonth) {
        return totalsByMonth.entrySet().stream()
                .map(entry -> new MonthlyCountReportRow(entry.getKey(), entry.getValue()))
                .toList();
    }

    private LinkedHashMap<YearMonth, BigDecimal> initializeAmountBuckets(YearMonth fromMonth, YearMonth toMonth) {
        validateMonthRange(fromMonth, toMonth);

        LinkedHashMap<YearMonth, BigDecimal> buckets = new LinkedHashMap<>();
        YearMonth currentMonth = fromMonth;
        while (!currentMonth.isAfter(toMonth)) {
            buckets.put(currentMonth, BigDecimal.ZERO);
            currentMonth = currentMonth.plusMonths(1);
        }
        return buckets;
    }

    private LinkedHashMap<YearMonth, Long> initializeCountBuckets(YearMonth fromMonth, YearMonth toMonth) {
        validateMonthRange(fromMonth, toMonth);

        LinkedHashMap<YearMonth, Long> buckets = new LinkedHashMap<>();
        YearMonth currentMonth = fromMonth;
        while (!currentMonth.isAfter(toMonth)) {
            buckets.put(currentMonth, 0L);
            currentMonth = currentMonth.plusMonths(1);
        }
        return buckets;
    }

    private DateRange monthRange(YearMonth fromMonth, YearMonth toMonth) {
        validateMonthRange(fromMonth, toMonth);
        return new DateRange(fromMonth.atDay(1), toMonth.atEndOfMonth());
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        Objects.requireNonNull(fromDate, "fromDate is required");
        Objects.requireNonNull(toDate, "toDate is required");
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must be before or equal to toDate");
        }
    }

    private void validateMonthRange(YearMonth fromMonth, YearMonth toMonth) {
        Objects.requireNonNull(fromMonth, "fromMonth is required");
        Objects.requireNonNull(toMonth, "toMonth is required");
        if (fromMonth.isAfter(toMonth)) {
            throw new IllegalArgumentException("fromMonth must be before or equal to toMonth");
        }
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private record DateRange(LocalDate fromDate, LocalDate toDate) {
    }

    private record ServiceMetadata(String serviceName, String categoryName, Integer durationMinutes) {
    }

    private record ProductMetadata(String productName, String categoryName) {
    }

    private static final class ServiceAccumulator {

        private final Long serviceId;
        private String snapshotName;
        private long confirmedAppointments;

        private ServiceAccumulator(Long serviceId, String snapshotName) {
            this.serviceId = serviceId;
            this.snapshotName = snapshotName;
        }

        private void registerAppointment(String serviceName) {
            if (this.snapshotName == null || this.snapshotName.isBlank()) {
                this.snapshotName = serviceName;
            }
            this.confirmedAppointments++;
        }
    }

    private static final class ProductAccumulator {

        private final Long productId;
        private String snapshotName;
        private long unitsSold;
        private BigDecimal generatedIncome = BigDecimal.ZERO;

        private ProductAccumulator(Long productId, String snapshotName) {
            this.productId = productId;
            this.snapshotName = snapshotName;
        }

        private void registerSale(String productName, Integer quantity, BigDecimal totalAmount) {
            if (this.snapshotName == null || this.snapshotName.isBlank()) {
                this.snapshotName = productName;
            }
            this.unitsSold += quantity != null ? quantity.longValue() : 0L;
            this.generatedIncome = this.generatedIncome.add(totalAmount != null ? totalAmount : BigDecimal.ZERO);
        }
    }
}
