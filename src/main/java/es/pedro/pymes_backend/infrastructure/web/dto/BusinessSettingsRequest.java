package es.pedro.pymes_backend.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessSettingsRequest {

    @NotBlank(message = "El nombre del negocio es obligatorio")
    @Size(max = 150, message = "El nombre del negocio no puede superar los 150 caracteres")
    private String businessName;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
    private String phone;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    private String address;

    @NotBlank(message = "El email de contacto es obligatorio")
    @Email(message = "El email de contacto no es válido")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    private String contactEmail;

    @NotNull(message = "El estado de la reserva online es obligatorio")
    private Boolean onlineBookingEnabled;

    @NotNull(message = "La duración predeterminada es obligatoria")
    @Min(value = 5, message = "La duración predeterminada debe ser al menos 5 minutos")
    private Integer defaultAppointmentDurationMinutes;

    @NotNull(message = "La antelación mínima es obligatoria")
    @Min(value = 0, message = "La antelación mínima no puede ser negativa")
    private Integer minimumAdvanceMinutes;

    @NotNull(message = "La antelación máxima es obligatoria")
    @Min(value = 0, message = "La antelación máxima no puede ser negativa")
    private Integer maximumAdvanceDays;

    @NotNull(message = "El límite de cancelación gratuita es obligatorio")
    @Min(value = 0, message = "El límite de cancelación gratuita no puede ser negativo")
    private Integer freeCancellationHours;

    @Size(max = 5000, message = "La política de cancelación no puede superar los 5000 caracteres")
    private String cancellationPolicy;

    @NotNull(message = "El horario semanal es obligatorio")
    @Size(min = 7, max = 7, message = "El horario semanal debe contener exactamente 7 días")
    @Valid
    private List<BusinessOpeningHoursDto> openingHours;

    @NotNull(message = "La lista de cierres especiales es obligatoria")
    @Valid
    private List<BusinessClosureDto> specialClosures;
}