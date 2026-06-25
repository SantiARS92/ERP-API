package es.supplesoftware.autonomos_api.services.application.dto;
import es.supplesoftware.autonomos_api.categories.application.dto.CategorySummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ServiceResponse {

    private Long id;
    private String name;
    private String description;
    private Integer durationMinutes;
    private Double price;
    private Boolean active;
    private CategorySummaryResponse category;
}