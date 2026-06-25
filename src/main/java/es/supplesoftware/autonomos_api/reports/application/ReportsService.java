package es.supplesoftware.autonomos_api.reports.application;
import es.supplesoftware.autonomos_api.reports.infrastructure.ReportQueryRepository;
import es.supplesoftware.autonomos_api.reports.application.dto.DayOfWeekAppointmentCountReportRow;
import es.supplesoftware.autonomos_api.reports.application.dto.MonthlyCountReportRow;
import es.supplesoftware.autonomos_api.reports.application.dto.MonthlyOperationalSummaryReportRow;
import es.supplesoftware.autonomos_api.reports.application.dto.ProductMonthlyRankingReportRow;
import es.supplesoftware.autonomos_api.reports.application.dto.ReportPeriodTotals;
import es.supplesoftware.autonomos_api.reports.application.dto.ServiceMonthlyPerformanceReportRow;
import es.supplesoftware.autonomos_api.reports.application.dto.AnnualConsolidatedReportResponse;
import es.supplesoftware.autonomos_api.reports.application.dto.AnnualMonthlyBreakdownResponse;
import es.supplesoftware.autonomos_api.reports.application.dto.AnnualProfitEvolutionPointResponse;
import es.supplesoftware.autonomos_api.reports.application.dto.BusiestDayReportResponse;
import es.supplesoftware.autonomos_api.reports.application.dto.MonthlyDetailedReportResponse;
import es.supplesoftware.autonomos_api.reports.application.dto.MonthlyReportPointResponse;
import es.supplesoftware.autonomos_api.reports.application.dto.MonthlySimpleReportResponse;
import es.supplesoftware.autonomos_api.reports.application.dto.QuarterlyDetailedReportResponse;
import es.supplesoftware.autonomos_api.reports.application.dto.QuarterlyMonthlyBreakdownResponse;
import es.supplesoftware.autonomos_api.reports.application.dto.QuarterlyProfitEvolutionPointResponse;
import es.supplesoftware.autonomos_api.reports.application.dto.RankingReportResponse;
import es.supplesoftware.autonomos_api.reports.application.dto.ReportRankingItemResponse;
import es.supplesoftware.autonomos_api.reports.domain.enums.ReportRankingType;

import es.supplesoftware.autonomos_api.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportsService {

    private static final BigDecimal VAT_DIVISOR = new BigDecimal("1.21");
    private static final BigDecimal IRPF_RATE = new BigDecimal("0.15");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final Locale LOCALE_ES = new Locale("es", "ES");

    private final ReportQueryRepository reportQueryRepository;

    public MonthlySimpleReportResponse getMonthlySimpleReport(int year, int month) {
        YearMonth targetMonth = buildYearMonth(year, month);
        YearMonth previousMonth = targetMonth.minusMonths(1);
        YearMonth firstChartMonth = targetMonth.minusMonths(2);

        List<SummaryMonth> lastThreeMonthsSummary = buildSummarySeries(firstChartMonth, targetMonth);
        SummaryMonth currentMonthSummary = findSummaryMonth(lastThreeMonthsSummary, targetMonth);
        SummaryMonth previousMonthSummary = findSummaryMonth(lastThreeMonthsSummary, previousMonth);

        Map<YearMonth, Long> appointmentsByMonth = reportQueryRepository
                .findMonthlyConfirmedAppointmentsEvolution(firstChartMonth, targetMonth)
                .stream()
                .collect(Collectors.toMap(
                        MonthlyCountReportRow::month,
                        MonthlyCountReportRow::count
                ));

        long currentConfirmedAppointments = appointmentsByMonth.getOrDefault(targetMonth, 0L);
        long previousConfirmedAppointments = appointmentsByMonth.getOrDefault(previousMonth, 0L);

        List<MonthlyReportPointResponse> lastThreeMonths = lastThreeMonthsSummary.stream()
                .map(row -> new MonthlyReportPointResponse(
                        row.month().toString(),
                        row.month().getYear(),
                        row.month().getMonthValue(),
                        row.income(),
                        row.expenses(),
                        appointmentsByMonth.getOrDefault(row.month(), 0L)
                ))
                .toList();

        List<ReportRankingItemResponse> topServices = reportQueryRepository.findTopServicesOfMonth(targetMonth).stream()
                .limit(4)
                .map(this::toRankingItemForTopServices)
                .toList();

        List<BusiestDayReportResponse> busiestDays = buildBusiestDays(targetMonth, currentConfirmedAppointments);

        return new MonthlySimpleReportResponse(
                year,
                month,
                currentMonthSummary.income(),
                currentMonthSummary.expenses(),
                currentConfirmedAppointments,
                calculateChangePercentage(currentMonthSummary.income(), previousMonthSummary.income()),
                calculateChangePercentage(currentMonthSummary.expenses(), previousMonthSummary.expenses()),
                calculateChangePercentage(
                        BigDecimal.valueOf(currentConfirmedAppointments),
                        BigDecimal.valueOf(previousConfirmedAppointments)
                ),
                lastThreeMonths,
                topServices,
                busiestDays
        );
    }

    public MonthlyDetailedReportResponse getMonthlyDetailedReport(int year, int month) {
        YearMonth targetMonth = buildYearMonth(year, month);

        ReportPeriodTotals totals = reportQueryRepository.findPeriodTotals(
                targetMonth.atDay(1),
                targetMonth.atEndOfMonth()
        );

        FiscalEstimate fiscalEstimate = calculateFiscalEstimate(
                totals.totalSales(),
                totals.totalExpenses()
        );

        return new MonthlyDetailedReportResponse(
                year,
                month,
                fiscalEstimate.grossIncome(),
                fiscalEstimate.grossExpenses(),
                fiscalEstimate.operatingProfit(),
                fiscalEstimate.estimatedIncomeBase(),
                fiscalEstimate.estimatedExpenseBase(),
                fiscalEstimate.estimatedOutputVat(),
                fiscalEstimate.estimatedInputVat(),
                fiscalEstimate.estimatedVatPayable(),
                fiscalEstimate.estimatedIrpfPayable(),
                fiscalEstimate.estimatedTotalTaxes(),
                fiscalEstimate.profitabilityRatio()
        );
    }

    public QuarterlyDetailedReportResponse getQuarterlyDetailedReport(int year, int quarter) {
        QuarterRange currentQuarter = buildQuarterRange(year, quarter);

        List<SummaryMonth> currentRows = buildSummarySeries(currentQuarter.startMonth(), currentQuarter.endMonth());
        List<SummaryMonth> previousRows = buildSummarySeries(
                currentQuarter.startMonth().minusMonths(3),
                currentQuarter.endMonth().minusMonths(3)
        );

        BigDecimal grossIncome = sumIncome(currentRows);
        BigDecimal grossExpenses = sumExpenses(currentRows);

        FiscalEstimate currentFiscalEstimate = calculateFiscalEstimate(grossIncome, grossExpenses);
        BigDecimal previousQuarterProfit = sumOperatingProfit(previousRows);

        List<QuarterlyMonthlyBreakdownResponse> monthlyBreakdown = currentRows.stream()
                .map(row -> {
                    FiscalEstimate monthFiscalEstimate = calculateFiscalEstimate(row.income(), row.expenses());

                    return new QuarterlyMonthlyBreakdownResponse(
                            row.month().getYear(),
                            row.month().getMonthValue(),
                            formatMonthLabel(row.month()),
                            row.income(),
                            row.expenses(),
                            row.operatingProfit(),
                            monthFiscalEstimate.estimatedVatPayable()
                    );
                })
                .toList();

        List<QuarterlyProfitEvolutionPointResponse> profitEvolution = currentRows.stream()
                .map(row -> new QuarterlyProfitEvolutionPointResponse(
                        row.month().getYear(),
                        row.month().getMonthValue(),
                        formatMonthLabel(row.month()),
                        row.operatingProfit()
                ))
                .toList();

        return new QuarterlyDetailedReportResponse(
                year,
                quarter,
                formatQuarterLabel(year, quarter),
                currentFiscalEstimate.grossIncome(),
                currentFiscalEstimate.grossExpenses(),
                currentFiscalEstimate.operatingProfit(),
                currentFiscalEstimate.estimatedIncomeBase(),
                currentFiscalEstimate.estimatedExpenseBase(),
                currentFiscalEstimate.estimatedOutputVat(),
                currentFiscalEstimate.estimatedInputVat(),
                currentFiscalEstimate.estimatedVatPayable(),
                currentFiscalEstimate.estimatedIrpfPayable(),
                currentFiscalEstimate.estimatedTotalTaxes(),
                currentFiscalEstimate.profitabilityRatio(),
                calculateChangePercentage(currentFiscalEstimate.operatingProfit(), previousQuarterProfit),
                monthlyBreakdown,
                profitEvolution
        );
    }

    public AnnualConsolidatedReportResponse getAnnualConsolidatedReport(int year) {
        validateYear(year);

        YearMonth startMonth = YearMonth.of(year, 1);
        YearMonth endMonth = YearMonth.of(year, 12);

        List<SummaryMonth> currentYearRows = buildSummarySeries(startMonth, endMonth);
        List<SummaryMonth> previousYearRows = buildSummarySeries(
                YearMonth.of(year - 1, 1),
                YearMonth.of(year - 1, 12)
        );

        BigDecimal grossIncome = sumIncome(currentYearRows);
        BigDecimal grossExpenses = sumExpenses(currentYearRows);

        FiscalEstimate currentFiscalEstimate = calculateFiscalEstimate(grossIncome, grossExpenses);
        BigDecimal previousYearProfit = sumOperatingProfit(previousYearRows);

        List<AnnualMonthlyBreakdownResponse> monthlyBreakdown = currentYearRows.stream()
                .map(row -> {
                    FiscalEstimate monthFiscalEstimate = calculateFiscalEstimate(row.income(), row.expenses());

                    return new AnnualMonthlyBreakdownResponse(
                            row.month().getYear(),
                            row.month().getMonthValue(),
                            formatMonthLabel(row.month()),
                            row.income(),
                            row.expenses(),
                            row.operatingProfit(),
                            monthFiscalEstimate.estimatedVatPayable(),
                            monthFiscalEstimate.estimatedIrpfPayable()
                    );
                })
                .toList();

        List<AnnualProfitEvolutionPointResponse> profitEvolution = currentYearRows.stream()
                .map(row -> new AnnualProfitEvolutionPointResponse(
                        row.month().getYear(),
                        row.month().getMonthValue(),
                        formatMonthLabel(row.month()),
                        row.operatingProfit()
                ))
                .toList();

        return new AnnualConsolidatedReportResponse(
                year,
                currentFiscalEstimate.grossIncome(),
                currentFiscalEstimate.grossExpenses(),
                currentFiscalEstimate.operatingProfit(),
                currentFiscalEstimate.estimatedIncomeBase(),
                currentFiscalEstimate.estimatedExpenseBase(),
                currentFiscalEstimate.estimatedOutputVat(),
                currentFiscalEstimate.estimatedInputVat(),
                currentFiscalEstimate.estimatedVatPayable(),
                currentFiscalEstimate.estimatedIrpfPayable(),
                currentFiscalEstimate.estimatedTotalTaxes(),
                currentFiscalEstimate.profitabilityRatio(),
                calculateChangePercentage(currentFiscalEstimate.operatingProfit(), previousYearProfit),
                monthlyBreakdown,
                profitEvolution
        );
    }

    public RankingReportResponse getRanking(ReportRankingType type, int year, int month, int page, int size) {
        if (type == null) {
            throw new BadRequestException("El tipo de ranking es obligatorio");
        }
        if (page < 0) {
            throw new BadRequestException("La página no puede ser negativa");
        }
        if (size <= 0) {
            throw new BadRequestException("El tamaño de página debe ser mayor que 0");
        }
        if (size > 100) {
            throw new BadRequestException("El tamaño de página no puede superar 100");
        }

        YearMonth targetMonth = buildYearMonth(year, month);

        List<ReportRankingItemResponse> allItems = switch (type) {
            case SERVICE -> buildServiceRanking(targetMonth);
            case PRODUCT -> buildProductRanking(targetMonth);
        };

        long totalElements = allItems.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        int fromIndex = page * size;
        if (fromIndex >= allItems.size()) {
            return new RankingReportResponse(
                    type,
                    year,
                    month,
                    List.of(),
                    page,
                    size,
                    totalElements,
                    totalPages,
                    page == 0,
                    true
            );
        }

        int toIndex = Math.min(fromIndex + size, allItems.size());
        List<ReportRankingItemResponse> pagedContent = allItems.subList(fromIndex, toIndex);

        return new RankingReportResponse(
                type,
                year,
                month,
                pagedContent,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                toIndex >= allItems.size()
        );
    }

    private List<ReportRankingItemResponse> buildServiceRanking(YearMonth targetMonth) {
        List<ServiceMonthlyPerformanceReportRow> rows = reportQueryRepository.findTopServicesOfMonth(targetMonth);

        return rows.stream()
                .map(this::toRankingItemForServicesWithoutPosition)
                .collect(Collectors.toList())
                .stream()
                .map(new PositionAssigner())
                .toList();
    }

    private List<ReportRankingItemResponse> buildProductRanking(YearMonth targetMonth) {
        List<ProductMonthlyRankingReportRow> rows = reportQueryRepository.findProductRankingOfMonth(targetMonth);

        return rows.stream()
                .map(this::toRankingItemForProductsWithoutPosition)
                .collect(Collectors.toList())
                .stream()
                .map(new PositionAssigner())
                .toList();
    }

    private ReportRankingItemResponse toRankingItemForTopServices(ServiceMonthlyPerformanceReportRow row) {
        return new ReportRankingItemResponse(
                null,
                row.serviceId(),
                row.serviceName(),
                row.categoryName(),
                row.confirmedAppointments(),
                scale(row.generatedIncome()),
                row.averageDurationMinutes()
        );
    }

    private ReportRankingItemResponse toRankingItemForServicesWithoutPosition(ServiceMonthlyPerformanceReportRow row) {
        return new ReportRankingItemResponse(
                null,
                row.serviceId(),
                row.serviceName(),
                row.categoryName(),
                row.confirmedAppointments(),
                scale(row.generatedIncome()),
                row.averageDurationMinutes()
        );
    }

    private ReportRankingItemResponse toRankingItemForProductsWithoutPosition(ProductMonthlyRankingReportRow row) {
        return new ReportRankingItemResponse(
                null,
                row.productId(),
                row.productName(),
                row.categoryName(),
                row.unitsSold(),
                scale(row.generatedIncome()),
                null
        );
    }

    private List<BusiestDayReportResponse> buildBusiestDays(YearMonth month, long totalConfirmedAppointments) {
        List<DayOfWeekAppointmentCountReportRow> rows = reportQueryRepository.countConfirmedAppointmentsByDayOfWeek(month);

        return rows.stream()
                .sorted(Comparator
                        .comparingLong(DayOfWeekAppointmentCountReportRow::confirmedAppointments)
                        .reversed()
                        .thenComparing(row -> row.dayOfWeek().getValue()))
                .limit(3)
                .map(row -> new BusiestDayReportResponse(
                        row.dayOfWeek(),
                        row.confirmedAppointments(),
                        calculatePercentage(row.confirmedAppointments(), totalConfirmedAppointments)
                ))
                .toList();
    }

    private SummaryMonth findSummaryMonth(List<SummaryMonth> rows, YearMonth month) {
        return rows.stream()
                .filter(row -> row.month().equals(month))
                .findFirst()
                .orElseGet(() -> new SummaryMonth(month, zero(), zero(), zero()));
    }

    private List<SummaryMonth> buildSummarySeries(YearMonth startMonth, YearMonth endMonth) {
        List<MonthlyOperationalSummaryReportRow> rawRows =
                reportQueryRepository.findMonthlyOperationalSummary(startMonth, endMonth);

        Map<YearMonth, MonthlyOperationalSummaryReportRow> rowsByMonth = rawRows.stream()
                .collect(Collectors.toMap(
                        MonthlyOperationalSummaryReportRow::month,
                        Function.identity()
                ));

        List<SummaryMonth> result = new ArrayList<>();
        YearMonth current = startMonth;

        while (!current.isAfter(endMonth)) {
            MonthlyOperationalSummaryReportRow row = rowsByMonth.get(current);

            BigDecimal income = row != null ? scale(row.income()) : zero();
            BigDecimal expenses = row != null ? scale(row.expenses()) : zero();
            BigDecimal operatingProfit = row != null
                    ? scale(row.operationalProfit())
                    : scale(income.subtract(expenses));

            result.add(new SummaryMonth(current, income, expenses, operatingProfit));
            current = current.plusMonths(1);
        }

        return result;
    }

    private FiscalEstimate calculateFiscalEstimate(BigDecimal grossIncome, BigDecimal grossExpenses) {
        BigDecimal safeGrossIncome = scale(grossIncome);
        BigDecimal safeGrossExpenses = scale(grossExpenses);
        BigDecimal operatingProfit = scale(safeGrossIncome.subtract(safeGrossExpenses));

        BigDecimal estimatedIncomeBase = divideGrossAmount(safeGrossIncome);
        BigDecimal estimatedExpenseBase = divideGrossAmount(safeGrossExpenses);

        BigDecimal estimatedOutputVat = scale(safeGrossIncome.subtract(estimatedIncomeBase));
        BigDecimal estimatedInputVat = scale(safeGrossExpenses.subtract(estimatedExpenseBase));
        BigDecimal estimatedVatPayable = scale(estimatedOutputVat.subtract(estimatedInputVat));

        BigDecimal estimatedTaxableProfit = scale(estimatedIncomeBase.subtract(estimatedExpenseBase));
        BigDecimal estimatedIrpfPayable = estimatedTaxableProfit.compareTo(BigDecimal.ZERO) > 0
                ? scale(estimatedTaxableProfit.multiply(IRPF_RATE))
                : zero();

        BigDecimal estimatedTotalTaxes = scale(estimatedVatPayable.add(estimatedIrpfPayable));

        BigDecimal profitabilityRatio = safeGrossIncome.compareTo(BigDecimal.ZERO) > 0
                ? scale(operatingProfit.multiply(ONE_HUNDRED).divide(safeGrossIncome, 2, RoundingMode.HALF_UP))
                : zero();

        return new FiscalEstimate(
                safeGrossIncome,
                safeGrossExpenses,
                operatingProfit,
                estimatedIncomeBase,
                estimatedExpenseBase,
                estimatedOutputVat,
                estimatedInputVat,
                estimatedVatPayable,
                estimatedIrpfPayable,
                estimatedTotalTaxes,
                profitabilityRatio
        );
    }

    private QuarterRange buildQuarterRange(int year, int quarter) {
        validateYear(year);

        if (quarter < 1 || quarter > 4) {
            throw new BadRequestException("El trimestre debe estar entre 1 y 4");
        }

        int startMonthValue = ((quarter - 1) * 3) + 1;
        YearMonth startMonth = YearMonth.of(year, startMonthValue);
        YearMonth endMonth = startMonth.plusMonths(2);

        return new QuarterRange(year, quarter, startMonth, endMonth);
    }

    private YearMonth buildYearMonth(int year, int month) {
        validateYear(year);

        if (month < 1 || month > 12) {
            throw new BadRequestException("El mes debe estar entre 1 y 12");
        }

        return YearMonth.of(year, month);
    }

    private void validateYear(int year) {
        if (year < 2000 || year > 2100) {
            throw new BadRequestException("El año debe estar entre 2000 y 2100");
        }
    }

    private BigDecimal sumIncome(List<SummaryMonth> rows) {
        return scale(rows.stream()
                .map(SummaryMonth::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal sumExpenses(List<SummaryMonth> rows) {
        return scale(rows.stream()
                .map(SummaryMonth::expenses)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal sumOperatingProfit(List<SummaryMonth> rows) {
        return scale(rows.stream()
                .map(SummaryMonth::operatingProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private String formatMonthLabel(YearMonth month) {
        String label = month.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES);
        return label.substring(0, 1).toUpperCase(LOCALE_ES) + label.substring(1);
    }

    private String formatQuarterLabel(int year, int quarter) {
        return quarter + "º trimestre " + year;
    }

    private BigDecimal divideGrossAmount(BigDecimal grossAmount) {
        if (grossAmount == null || grossAmount.compareTo(BigDecimal.ZERO) == 0) {
            return zero();
        }

        return grossAmount.divide(VAT_DIVISOR, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateChangePercentage(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = current == null ? BigDecimal.ZERO : current;
        BigDecimal safePrevious = previous == null ? BigDecimal.ZERO : previous;

        if (safePrevious.compareTo(BigDecimal.ZERO) == 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) == 0 ? zero() : null;
        }

        return scale(
                safeCurrent.subtract(safePrevious)
                        .multiply(ONE_HUNDRED)
                        .divide(safePrevious, 2, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal calculatePercentage(long value, long total) {
        if (total <= 0) {
            return zero();
        }

        return scale(
                BigDecimal.valueOf(value)
                        .multiply(ONE_HUNDRED)
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return zero();
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private record QuarterRange(int year, int quarter, YearMonth startMonth, YearMonth endMonth) {
    }

    private record SummaryMonth(
            YearMonth month,
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal operatingProfit
    ) {
    }

    private record FiscalEstimate(
            BigDecimal grossIncome,
            BigDecimal grossExpenses,
            BigDecimal operatingProfit,
            BigDecimal estimatedIncomeBase,
            BigDecimal estimatedExpenseBase,
            BigDecimal estimatedOutputVat,
            BigDecimal estimatedInputVat,
            BigDecimal estimatedVatPayable,
            BigDecimal estimatedIrpfPayable,
            BigDecimal estimatedTotalTaxes,
            BigDecimal profitabilityRatio
    ) {
    }

    private static final class PositionAssigner implements Function<ReportRankingItemResponse, ReportRankingItemResponse> {

        private int currentPosition = 0;

        @Override
        public ReportRankingItemResponse apply(ReportRankingItemResponse item) {
            currentPosition++;
            item.setPosition(currentPosition);
            return item;
        }
    }
}
