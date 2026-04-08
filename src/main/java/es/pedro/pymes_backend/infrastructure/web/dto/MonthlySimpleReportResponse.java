package es.pedro.pymes_backend.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySimpleReportResponse {

    private Integer year;
    private Integer month;

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private Long totalConfirmedAppointments;

    private BigDecimal incomeChangePercentage;
    private BigDecimal expenseChangePercentage;
    private BigDecimal confirmedAppointmentsChangePercentage;

    private List<MonthlyReportPointResponse> lastThreeMonths;
    private List<ReportRankingItemResponse> topServices;
    private List<BusiestDayReportResponse> busiestDays;
}