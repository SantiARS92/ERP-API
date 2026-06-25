package es.supplesoftware.autonomos_api.professionals.application.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres")
    private String fullName;

    @NotBlank(message = "La especialidad o cargo es obligatoria")
    @Size(max = 150, message = "La especialidad o cargo no puede superar los 150 caracteres")
    private String specialty;

    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
    private String phone;

    @Email(message = "El email no es válido")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    private String email;

    @Size(max = 150, message = "La clave de acceso no puede superar los 150 caracteres")
    private String accessPassword;

    @NotNull(message = "El estado del profesional es obligatorio")
    private Boolean active;
}
