package es.pedro.pymes_backend.infrastructure.repository.report;

import java.time.YearMonth;

public record MonthlyCountReportRow(
        YearMonth month,
        long count
) {
}
