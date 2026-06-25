package es.supplesoftware.autonomos_api.sales.application.dto;
import es.supplesoftware.autonomos_api.sales.domain.enums.PaymentMethod;

import es.supplesoftware.autonomos_api.sales.domain.enums.SaleItemType;

import es.supplesoftware.autonomos_api.sales.domain.enums.SaleSourceType;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleResponse {

    private Long id;
    private SaleSourceType sourceType;
    private Long appointmentId;

    private Long clientId;
    private String clientName;

    private SaleItemType itemType;
    private Long serviceId;
    private Long inventoryProductId;
    private Long inventoryProductVariantId;
    private String inventoryProductVariantLabel;
    private String inventoryProductVariantSku;
    private String itemName;

    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalAmount;

    private PaymentMethod paymentMethod;

    private LocalDate saleDate;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
