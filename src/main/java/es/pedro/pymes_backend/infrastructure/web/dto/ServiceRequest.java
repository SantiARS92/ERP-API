package es.pedro.pymes_backend.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    private String description;

    @NotNull(message = "La duración es obligatoria")
    @Positive(message = "La duración debe ser mayor que 0")
    private Integer durationMinutes;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor que 0")
    private Double price;

    @NotNull(message = "El estado es obligatorio")
    private Boolean active;
}