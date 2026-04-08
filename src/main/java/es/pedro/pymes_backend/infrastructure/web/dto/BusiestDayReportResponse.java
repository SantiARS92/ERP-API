package es.pedro.pymes_backend.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusiestDayReportResponse {

    private DayOfWeek dayOfWeek;
    private Long confirmedAppointments;
    private BigDecimal percentage;
}