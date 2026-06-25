package es.supplesoftware.autonomos_api.reports.infrastructure;
import es.supplesoftware.autonomos_api.appointments.domain.enums.AppointmentStatus;

import es.supplesoftware.autonomos_api.sales.domain.enums.SaleItemType;

import es.supplesoftware.autonomos_api.sales.domain.enums.SaleSourceType;

import es.supplesoftware.autonomos_api.reports.application.dto.DayOfWeekAppointmentCountReportRow;
import es.supplesoftware.autonomos_api.reports.application.dto.MonthlyAmountReportRow;
import es.supplesoftware.autonomos_api.reports.application.dto.MonthlyCountReportRow;
import es.supplesoftware.autonomos_api.reports.application.dto.MonthlyOperationalSummaryReportRow;
import es.supplesoftware.autonomos_api.reports.application.dto.ProductMonthlyRankingReportRow;
import es.supplesoftware.autonomos_api.reports.application.dto.ReportPeriodTotals;
import es.supplesoftware.autonomos_api.reports.application.dto.ServiceMonthlyPerformanceReportRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import es.supplesoftware.autonomos_api.appointments.infrastructure.AppointmentEntity;
import es.supplesoftware.autonomos_api.expenses.infrastructure.ExpenseEntity;
import es.supplesoftware.autonomos_api.sales.infrastructure.SaleEntity;

@Repository
@Transactional(readOnly = true)
public class ReportQueryRepository {

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
        for (Tuple row : findConfirmedAppointmentCountsByDayOfWeekBetween(dateRange.fromDate(), dateRange.toDate())) {
            DayOfWeek dayOfWeek = toJavaDayOfWeek(row.get("dayOfWeekValue", Number.class));
            Long count = row.get("countValue", Long.class);
            totalsByDay.put(dayOfWeek, count != null ? count : 0L);
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
        return entityManager.createQuery("""
                        select new es.supplesoftware.autonomos_api.reports.application.dto.ServiceMonthlyPerformanceReportRow(
                            srv.id,
                            srv.name,
                            category.name,
                            srv.durationMinutes,
                            count(s.id),
                            sum(s.totalAmount)
                        )
                        from SaleEntity s
                        join s.service srv
                        left join srv.category category
                        where s.itemType = :serviceType
                          and s.sourceType = :sourceType
                          and s.saleDate between :fromDate and :toDate
                        group by srv.id, srv.name, category.name, srv.durationMinutes
                        order by count(s.id) desc, sum(s.totalAmount) desc, lower(srv.name) asc
                        """, ServiceMonthlyPerformanceReportRow.class)
                .setParameter("serviceType", SaleItemType.SERVICE)
                .setParameter("sourceType", SaleSourceType.APPOINTMENT)
                .setParameter("fromDate", dateRange.fromDate())
                .setParameter("toDate", dateRange.toDate())
                .getResultList();
    }

    public List<ProductMonthlyRankingReportRow> findProductRankingOfMonth(YearMonth month) {
        Objects.requireNonNull(month, "month is required");

        DateRange dateRange = monthRange(month, month);
        return entityManager.createQuery("""
                        select new es.supplesoftware.autonomos_api.reports.application.dto.ProductMonthlyRankingReportRow(
                            product.id,
                            product.name,
                            category.name,
                            sum(s.quantity),
                            sum(s.totalAmount)
                        )
                        from SaleEntity s
                        join s.inventoryProduct product
                        left join product.category category
                        where s.itemType = :productType
                          and s.saleDate between :fromDate and :toDate
                        group by product.id, product.name, category.name
                        order by sum(s.quantity) desc, sum(s.totalAmount) desc, lower(product.name) asc
                        """, ProductMonthlyRankingReportRow.class)
                .setParameter("productType", SaleItemType.PRODUCT)
                .setParameter("fromDate", dateRange.fromDate())
                .setParameter("toDate", dateRange.toDate())
                .getResultList();
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

        for (Tuple row : findMonthlySaleAmountRowsBetween(dateRange.fromDate(), dateRange.toDate())) {
            YearMonth month = toYearMonth(row);
            BigDecimal amount = row.get("amount", BigDecimal.class);
            totals.put(month, amountOrZero(amount));
        }

        return totals;
    }

    private LinkedHashMap<YearMonth, BigDecimal> aggregateExpensesByMonth(YearMonth fromMonth, YearMonth toMonth) {
        DateRange dateRange = monthRange(fromMonth, toMonth);
        LinkedHashMap<YearMonth, BigDecimal> totals = initializeAmountBuckets(fromMonth, toMonth);

        for (Tuple row : findMonthlyExpenseAmountRowsBetween(dateRange.fromDate(), dateRange.toDate())) {
            YearMonth month = toYearMonth(row);
            BigDecimal amount = row.get("amount", BigDecimal.class);
            totals.put(month, amountOrZero(amount));
        }

        return totals;
    }

    private LinkedHashMap<YearMonth, Long> aggregateConfirmedAppointmentsByMonth(YearMonth fromMonth, YearMonth toMonth) {
        DateRange dateRange = monthRange(fromMonth, toMonth);
        LinkedHashMap<YearMonth, Long> totals = initializeCountBuckets(fromMonth, toMonth);

        for (Tuple row : findMonthlyConfirmedAppointmentRowsBetween(dateRange.fromDate(), dateRange.toDate())) {
            YearMonth month = toYearMonth(row);
            Long count = row.get("countValue", Long.class);
            totals.put(month, count != null ? count : 0L);
        }

        return totals;
    }

    private List<Tuple> findMonthlySaleAmountRowsBetween(LocalDate fromDate, LocalDate toDate) {
        return entityManager.createQuery("""
                        select
                            year(s.saleDate) as yearValue,
                            month(s.saleDate) as monthValue,
                            sum(s.totalAmount) as amount
                        from SaleEntity s
                        where s.saleDate between :fromDate and :toDate
                        group by year(s.saleDate), month(s.saleDate)
                        order by year(s.saleDate) asc, month(s.saleDate) asc
                        """, Tuple.class)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();
    }

    private List<Tuple> findMonthlyExpenseAmountRowsBetween(LocalDate fromDate, LocalDate toDate) {
        return entityManager.createQuery("""
                        select
                            year(e.expenseDate) as yearValue,
                            month(e.expenseDate) as monthValue,
                            sum(e.amount) as amount
                        from ExpenseEntity e
                        where e.expenseDate between :fromDate and :toDate
                        group by year(e.expenseDate), month(e.expenseDate)
                        order by year(e.expenseDate) asc, month(e.expenseDate) asc
                        """, Tuple.class)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();
    }

    private List<Tuple> findMonthlyConfirmedAppointmentRowsBetween(LocalDate fromDate, LocalDate toDate) {
        return entityManager.createQuery("""
                        select
                            year(a.appointmentDate) as yearValue,
                            month(a.appointmentDate) as monthValue,
                            count(a.id) as countValue
                        from AppointmentEntity a
                        where a.status = :status
                          and a.appointmentDate between :fromDate and :toDate
                        group by year(a.appointmentDate), month(a.appointmentDate)
                        order by year(a.appointmentDate) asc, month(a.appointmentDate) asc
                        """, Tuple.class)
                .setParameter("status", AppointmentStatus.CONFIRMED)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();
    }

    private List<Tuple> findConfirmedAppointmentCountsByDayOfWeekBetween(LocalDate fromDate, LocalDate toDate) {
        return entityManager.createQuery("""
                        select
                            extract(day of week from a.appointmentDate) as dayOfWeekValue,
                            count(a.id) as countValue
                        from AppointmentEntity a
                        where a.status = :status
                          and a.appointmentDate between :fromDate and :toDate
                        group by extract(day of week from a.appointmentDate)
                        order by extract(day of week from a.appointmentDate) asc
                        """, Tuple.class)
                .setParameter("status", AppointmentStatus.CONFIRMED)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();
    }

    private YearMonth toYearMonth(Tuple row) {
        Number yearValue = row.get("yearValue", Number.class);
        Number monthValue = row.get("monthValue", Number.class);
        return YearMonth.of(yearValue.intValue(), monthValue.intValue());
    }

    private DayOfWeek toJavaDayOfWeek(Number hqlDayOfWeek) {
        if (hqlDayOfWeek == null) {
            throw new IllegalArgumentException("dayOfWeekValue is required");
        }

        return switch (hqlDayOfWeek.intValue()) {
            case 1 -> DayOfWeek.MONDAY;
            case 2 -> DayOfWeek.TUESDAY;
            case 3 -> DayOfWeek.WEDNESDAY;
            case 4 -> DayOfWeek.THURSDAY;
            case 5 -> DayOfWeek.FRIDAY;
            case 6 -> DayOfWeek.SATURDAY;
            case 7 -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Unsupported dayOfWeekValue: " + hqlDayOfWeek);
        };
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
}
