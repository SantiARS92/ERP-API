package es.pedro.pymes_backend.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportPointResponse {

    private String period;
    private Integer year;
    private Integer month;
    private BigDecimal income;
    private BigDecimal expenses;
    private Long confirmedAppointments;
}