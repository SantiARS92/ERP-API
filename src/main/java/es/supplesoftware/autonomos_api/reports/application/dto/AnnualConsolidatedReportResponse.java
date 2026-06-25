package es.supplesoftware.autonomos_api.reports.application.dto;
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
public class AnnualConsolidatedReportResponse {

    private Integer year;

    private BigDecimal grossIncome;
    private BigDecimal grossExpenses;
    private BigDecimal operatingProfit;

    private BigDecimal estimatedIncomeBase;
    private BigDecimal estimatedExpenseBase;

    private BigDecimal estimatedOutputVat;
    private BigDecimal estimatedInputVat;
    private BigDecimal estimatedVatPayable;

    private BigDecimal estimatedIrpfPayable;
    private BigDecimal estimatedTotalTaxes;

    private BigDecimal profitabilityRatio;
    private BigDecimal operatingProfitChangePercentage;

    private List<AnnualMonthlyBreakdownResponse> monthlyBreakdown;
    private List<AnnualProfitEvolutionPointResponse> profitEvolution;
}