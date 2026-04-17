package es.pedro.pymes_backend.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClientRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    private String fullName;

    @NotBlank(message = "El teléfono es obligatorio")
    private String phone;

    @Email(message = "El email debe tener un formato válido")
    private String email;

    @JsonProperty("admiteFacturas")
    private Boolean acceptsInvoices;

    @JsonProperty("direccion")
    private String address;

    private String cifNif;
}
