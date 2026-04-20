package es.pedro.pymes_backend.domain;

import es.pedro.pymes_backend.domain.ClientEntity;
import es.pedro.pymes_backend.domain.ServiceEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "sales",
        indexes = {
                @Index(name = "idx_sales_date_time_id", columnList = "sale_date, sale_time, id"),
                @Index(name = "idx_sales_item_source_date", columnList = "item_type, source_type, sale_date"),
                @Index(name = "idx_sales_service_date", columnList = "service_id, sale_date"),
                @Index(name = "idx_sales_inventory_date", columnList = "inventory_product_id, sale_date"),
                @Index(name = "idx_sales_inventory_variant_date", columnList = "inventory_product_variant_id, sale_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleSourceType sourceType;

    @Column(unique = true)
    private Long appointmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @Column(nullable = false)
    private String clientName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceEntity service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_product_id")
    private InventoryProductEntity inventoryProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_product_variant_id")
    private InventoryProductVariantEntity inventoryProductVariant;

    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private LocalDate saleDate;

    @Column(nullable = false)
    private LocalTime saleTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
