package es.pedro.pymes_backend.infrastructure.repository.report;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyAmountReportRow(
        YearMonth month,
        BigDecimal amount
) {
}
