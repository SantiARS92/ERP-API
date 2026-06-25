package es.supplesoftware.autonomos_api.expenses.infrastructure;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductEntity;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductVariantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import es.supplesoftware.autonomos_api.expenses.domain.enums.ExpenseCategory;

@Entity
@Table(
        name = "expenses",
        indexes = {
                @Index(name = "idx_expenses_date_id", columnList = "expense_date, id"),
                @Index(name = "idx_expenses_category_date", columnList = "category, expense_date"),
                @Index(name = "idx_expenses_inventory_product", columnList = "inventory_product_id"),
                @Index(name = "idx_expenses_inventory_product_variant", columnList = "inventory_product_variant_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(nullable = false, length = 150)
    private String concept;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseCategory category;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_product_id")
    private InventoryProductEntity inventoryProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_product_variant_id")
    private InventoryProductVariantEntity inventoryProductVariant;

    @Column(name = "stock_movement_applied", nullable = false)
    private Boolean stockMovementApplied;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.stockMovementApplied == null) {
            this.stockMovementApplied = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
