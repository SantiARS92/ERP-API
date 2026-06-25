package es.supplesoftware.autonomos_api.reports.application.dto;
import java.time.YearMonth;

public record MonthlyCountReportRow(
        YearMonth month,
        long count
) {
}
