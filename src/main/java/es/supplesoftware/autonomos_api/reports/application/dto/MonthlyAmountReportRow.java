package es.supplesoftware.autonomos_api.reports.application.dto;
import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyAmountReportRow(
        YearMonth month,
        BigDecimal amount
) {
}
