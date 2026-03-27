package es.pedro.pymes_backend.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessOpeningHoursDto {

    @NotNull(message = "El día de la semana es obligatorio")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "El estado del día es obligatorio")
    private Boolean enabled;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime openingTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime closingTime;
}