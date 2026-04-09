package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.infrastructure.security.CustomUserPrincipal;
import es.pedro.pymes_backend.infrastructure.web.dto.AuthMeResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.LoginRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse loginAsGuest();

    AuthMeResponse getAuthenticatedUser(CustomUserPrincipal principal);
}
