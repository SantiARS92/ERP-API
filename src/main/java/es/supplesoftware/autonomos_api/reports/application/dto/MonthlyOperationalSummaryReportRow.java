package es.supplesoftware.autonomos_api.reports.application.dto;
import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyOperationalSummaryReportRow(
        YearMonth month,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal operationalProfit
) {
}
