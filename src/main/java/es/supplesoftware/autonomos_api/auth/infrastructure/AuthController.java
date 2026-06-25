package es.supplesoftware.autonomos_api.auth.infrastructure;
import es.supplesoftware.autonomos_api.auth.application.AuthService;
import es.supplesoftware.autonomos_api.security.CustomUserPrincipal;
import es.supplesoftware.autonomos_api.auth.application.dto.AuthMeResponse;
import es.supplesoftware.autonomos_api.auth.application.dto.LoginRequest;
import es.supplesoftware.autonomos_api.auth.application.dto.LoginResponse;
import es.supplesoftware.autonomos_api.auth.application.dto.RefreshTokenRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/guest")
    public LoginResponse guest() {
        return authService.loginAsGuest();
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.getRefreshToken());
    }

    @GetMapping("/me")
    public AuthMeResponse me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return authService.getAuthenticatedUser(principal);
    }
}
