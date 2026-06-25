package es.supplesoftware.autonomos_api.products.application.dto;
import es.supplesoftware.autonomos_api.products.domain.enums.InventoryStockStatus;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryProductResponse {

    private Long id;
    private String name;
    private String description;

    private Long categoryId;
    private String categoryName;

    private Integer stock;
    private Integer minimumStock;

    private String sku;
    private BigDecimal purchasePrice;

    private Boolean hasVariants;
    private Integer variantCount;
    private List<InventoryProductVariantResponse> variants;

    private Boolean active;

    private InventoryStockStatus status;
    private String statusLabel;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
