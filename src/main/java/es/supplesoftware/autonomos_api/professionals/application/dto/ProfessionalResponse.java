package es.supplesoftware.autonomos_api.professionals.application.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalResponse {

    private Long id;
    private String fullName;
    private String specialty;
    private String phone;
    private String email;
    private Boolean hasAccessPassword;
    private Boolean active;
}
