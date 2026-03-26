package es.pedro.pymes_backend.infrastructure.web.dto;

import es.pedro.pymes_backend.domain.InventoryStockStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    private Boolean active;

    private InventoryStockStatus status;
    private String statusLabel;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}