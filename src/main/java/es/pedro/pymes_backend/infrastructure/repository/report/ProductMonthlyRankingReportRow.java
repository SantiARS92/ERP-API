package es.pedro.pymes_backend.infrastructure.repository.report;

import java.math.BigDecimal;

public record ProductMonthlyRankingReportRow(
        Long productId,
        String productName,
        String categoryName,
        long unitsSold,
        BigDecimal generatedIncome
) {
}
