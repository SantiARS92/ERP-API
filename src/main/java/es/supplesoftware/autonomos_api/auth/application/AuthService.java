package es.supplesoftware.autonomos_api.auth.application;
import es.supplesoftware.autonomos_api.security.CustomUserPrincipal;
import es.supplesoftware.autonomos_api.auth.application.dto.AuthMeResponse;
import es.supplesoftware.autonomos_api.auth.application.dto.LoginRequest;
import es.supplesoftware.autonomos_api.auth.application.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse loginAsGuest();

    LoginResponse refresh(String refreshToken);

    AuthMeResponse getAuthenticatedUser(CustomUserPrincipal principal);
}
