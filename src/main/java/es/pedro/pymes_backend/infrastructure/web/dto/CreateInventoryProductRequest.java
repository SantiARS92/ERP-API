package es.pedro.pymes_backend.infrastructure.web.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInventoryProductRequest {

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 150, message = "El nombre del producto no puede superar los 150 caracteres")
    private String name;

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    private String description;

    @NotNull(message = "La categoría es obligatoria")
    private Long categoryId;

    @NotNull(message = "El stock actual es obligatorio")
    @Min(value = 0, message = "El stock actual no puede ser negativo")
    private Integer stock;

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    private Integer minimumStock;

    @Size(max = 100, message = "El SKU no puede superar los 100 caracteres")
    private String sku;

    @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
    private BigDecimal purchasePrice;
}