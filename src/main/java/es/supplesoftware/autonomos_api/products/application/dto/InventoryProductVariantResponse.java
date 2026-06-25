package es.supplesoftware.autonomos_api.products.application.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryProductVariantResponse {

    private Long id;
    private String variantLabel;
    private String sku;
    private Integer stock;
    private Integer minimumStock;
    private BigDecimal purchasePrice;
    private Boolean active;
    private Map<String, String> attributes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
