package es.pedro.pymes_backend.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessClosureDto {

    @NotNull(message = "La fecha de cierre es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate closureDate;

    @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
    private String reason;
}