package es.pedro.pymes_backend.infrastructure.repository.report;

import java.math.BigDecimal;

public record ReportPeriodTotals(
        BigDecimal totalSales,
        BigDecimal totalExpenses,
        long confirmedAppointments
) {
}
