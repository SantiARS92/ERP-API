package es.supplesoftware.autonomos_api.categories.application.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    private String name;

    @NotBlank(message = "El color es obligatorio")
    private String color;

    @NotBlank(message = "El icono es obligatorio")
    private String icon;
}