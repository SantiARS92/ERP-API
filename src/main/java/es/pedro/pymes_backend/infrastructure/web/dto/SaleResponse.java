package es.pedro.pymes_backend.infrastructure.web.dto;

import es.pedro.pymes_backend.domain.PaymentMethod;
import es.pedro.pymes_backend.domain.SaleItemType;
import es.pedro.pymes_backend.domain.SaleSourceType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
    private String itemName;

    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalAmount;

    private PaymentMethod paymentMethod;

    private LocalDate saleDate;
    private LocalTime saleTime;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
