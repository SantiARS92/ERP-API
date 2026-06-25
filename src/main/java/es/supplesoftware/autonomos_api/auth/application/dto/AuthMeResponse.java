package es.supplesoftware.autonomos_api.auth.application.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthMeResponse {

    private String role;
    private String displayName;
    private Long professionalId;
}
