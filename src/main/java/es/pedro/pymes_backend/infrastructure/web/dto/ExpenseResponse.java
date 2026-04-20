package es.pedro.pymes_backend.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import es.pedro.pymes_backend.domain.ExpenseCategory;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expenseDate;

    private String concept;

    private ExpenseCategory category;

    private String categoryLabel;

    private Integer quantity;

    private BigDecimal amount;

    private String notes;

    private Long inventoryProductId;

    private Long inventoryProductVariantId;

    private String inventoryProductVariantLabel;

    private String inventoryProductVariantSku;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
