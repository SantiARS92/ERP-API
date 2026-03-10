package es.pedro.pymes_backend.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CategorySummaryResponse {

    private Long id;
    private String name;
    private String color;
    private String icon;
}