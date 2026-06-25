package es.supplesoftware.autonomos_api.reports.application.dto;
import java.math.BigDecimal;

public record ProductMonthlyRankingReportRow(
        Long productId,
        String productName,
        String categoryName,
        long unitsSold,
        BigDecimal generatedIncome
) {
}
