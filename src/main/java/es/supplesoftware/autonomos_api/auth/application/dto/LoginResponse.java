package es.supplesoftware.autonomos_api.auth.application.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private String role;
    private String displayName;
    private Long professionalId;
}
