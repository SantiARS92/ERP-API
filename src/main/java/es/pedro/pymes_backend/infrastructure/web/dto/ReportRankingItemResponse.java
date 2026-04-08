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
public class ReportRankingItemResponse {

    private Integer position;
    private Long id;
    private String name;
    private String categoryName;
    private Long totalCount;
    private BigDecimal generatedIncome;
    private Integer averageDurationMinutes;
}