package es.supplesoftware.autonomos_api.reports.application.dto;
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
