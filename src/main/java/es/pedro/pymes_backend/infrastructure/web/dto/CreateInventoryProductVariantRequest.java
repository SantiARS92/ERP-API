package es.pedro.pymes_backend.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInventoryProductVariantRequest {

    @NotBlank(message = "La etiqueta de la variante es obligatoria")
    @Size(max = 150, message = "La etiqueta de la variante no puede superar los 150 caracteres")
    private String variantLabel;

    @Size(max = 100, message = "El SKU no puede superar los 100 caracteres")
    private String sku;

    @NotNull(message = "El stock de la variante es obligatorio")
    @Min(value = 0, message = "El stock de la variante no puede ser negativo")
    private Integer stock;

    @NotNull(message = "El stock minimo de la variante es obligatorio")
    @Min(value = 0, message = "El stock minimo de la variante no puede ser negativo")
    private Integer minimumStock;

    @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
    private BigDecimal purchasePrice;

    private Map<String, String> attributes;
}
