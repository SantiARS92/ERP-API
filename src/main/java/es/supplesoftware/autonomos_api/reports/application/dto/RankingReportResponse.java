package es.supplesoftware.autonomos_api.reports.application.dto;
import es.supplesoftware.autonomos_api.reports.domain.enums.ReportRankingType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RankingReportResponse {

    private ReportRankingType type;
    private Integer year;
    private Integer month;

    private List<ReportRankingItemResponse> content;

    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private Boolean first;
    private Boolean last;
}