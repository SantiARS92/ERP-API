package es.pedro.pymes_backend.infrastructure.repository.report;

import java.math.BigDecimal;

public record ServiceMonthlyPerformanceReportRow(
        Long serviceId,
        String serviceName,
        String categoryName,
        Integer averageDurationMinutes,
        long confirmedAppointments,
        BigDecimal generatedIncome
) {
}
