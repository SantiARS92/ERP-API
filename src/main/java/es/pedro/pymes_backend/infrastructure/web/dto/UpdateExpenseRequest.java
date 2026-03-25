package es.pedro.pymes_backend.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import es.pedro.pymes_backend.domain.ExpenseCategory;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateExpenseRequest {

    @NotNull(message = "La fecha es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expenseDate;

    @NotBlank(message = "El concepto es obligatorio")
    @Size(max = 150, message = "El concepto no puede superar los 150 caracteres")
    private String concept;

    @NotNull(message = "La categoría es obligatoria")
    private ExpenseCategory category;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer quantity;

    @NotNull(message = "El importe es obligatorio")
    @DecimalMin(value = "0.01", message = "El importe debe ser mayor que 0")
    private BigDecimal amount;

    @Size(max = 1000, message = "Las notas no pueden superar los 1000 caracteres")
    private String notes;
}