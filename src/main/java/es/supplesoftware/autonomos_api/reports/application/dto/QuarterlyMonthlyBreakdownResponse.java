package es.supplesoftware.autonomos_api.reports.application.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuarterlyMonthlyBreakdownResponse {

    private Integer year;
    private Integer month;
    private String monthLabel;

    private BigDecimal grossIncome;
    private BigDecimal grossExpenses;
    private BigDecimal operatingProfit;
    private BigDecimal estimatedVatPayable;
}