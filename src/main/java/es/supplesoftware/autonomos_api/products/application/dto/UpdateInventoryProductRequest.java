package es.supplesoftware.autonomos_api.products.application.dto;
import jakarta.validation.Valid;
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
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInventoryProductRequest {

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 150, message = "El nombre del producto no puede superar los 150 caracteres")
    private String name;

    @Size(max = 1000, message = "La descripcion no puede superar los 1000 caracteres")
    private String description;

    @NotNull(message = "La categoria es obligatoria")
    private Long categoryId;

    @Min(value = 0, message = "El stock actual no puede ser negativo")
    private Integer stock;

    @Min(value = 0, message = "El stock minimo no puede ser negativo")
    private Integer minimumStock;

    @Size(max = 100, message = "El SKU no puede superar los 100 caracteres")
    private String sku;

    @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
    private BigDecimal purchasePrice;

    private Boolean hasVariants;

    @Valid
    private List<UpdateInventoryProductVariantRequest> variants;
}
