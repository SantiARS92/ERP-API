package es.supplesoftware.autonomos_api.reports.application.dto;
import java.math.BigDecimal;

public record ReportPeriodTotals(
        BigDecimal totalSales,
        BigDecimal totalExpenses,
        long confirmedAppointments
) {
}
