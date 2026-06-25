package es.supplesoftware.autonomos_api.sales.application.dto;
import es.supplesoftware.autonomos_api.sales.domain.enums.PaymentMethod;

import es.supplesoftware.autonomos_api.sales.domain.enums.SaleItemType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSaleRequest {

    private Long clientId;

    @NotNull(message = "El tipo de concepto es obligatorio")
    private SaleItemType itemType;

    private Long serviceId;

    private Long inventoryProductId;

    private Long inventoryProductVariantId;

    @Size(max = 255, message = "El nombre del producto no puede superar 255 caracteres")
    private String itemName;

    @DecimalMin(value = "0.00", inclusive = true, message = "El precio unitario no puede ser negativo")
    private BigDecimal unitPrice;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer quantity;

    private PaymentMethod paymentMethod;

    @NotNull(message = "La fecha de la venta es obligatoria")
    private LocalDate saleDate;

    @Size(max = 1000, message = "Las notas no pueden superar 1000 caracteres")
    private String notes;
}
