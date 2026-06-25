package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.ProfessionalEntity;
import es.pedro.pymes_backend.infrastructure.repository.ProfessionalRepository;
import es.pedro.pymes_backend.infrastructure.security.CustomUserPrincipal;
import es.pedro.pymes_backend.infrastructure.security.JwtService;
import es.pedro.pymes_backend.infrastructure.web.dto.AuthMeResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.LoginRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PROFESSIONAL_ROLE = "PROFESSIONAL";
    private static final String GUEST_ROLE = "GUEST";
    private static final String ADMIN_DISPLAY_NAME = "Administrador";
    private static final String GUEST_DISPLAY_NAME = "Invitado";

    private final JwtService jwtService;
    private final ProfessionalRepository professionalRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPasswordHash;

    public AuthServiceImpl(
            JwtService jwtService,
            ProfessionalRepository professionalRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.admin.username}") String adminUsername,
            @Value("${app.auth.admin.password}") String adminPassword
    ) {
        this.jwtService = jwtService;
        this.professionalRepository = professionalRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPasswordHash = passwordEncoder.encode(adminPassword);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String normalizedUsername = request.getUsername().trim();

        if (adminUsername.equals(normalizedUsername)) {
            if (!passwordEncoder.matches(request.getPassword(), adminPasswordHash)) {
                throw new BadCredentialsException("Credenciales invalidas");
            }

            CustomUserPrincipal principal = new CustomUserPrincipal(
                    adminUsername,
                    ADMIN_ROLE,
                    ADMIN_DISPLAY_NAME,
                    null
            );

            return buildLoginResponse(principal);
        }

        ProfessionalEntity professional = professionalRepository.findByNormalizedFullName(normalizedUsername)
                .filter(foundProfessional -> Boolean.TRUE.equals(foundProfessional.getActive()))
                .filter(foundProfessional -> StringUtils.hasText(foundProfessional.getPasswordHash()))
                .filter(foundProfessional -> passwordEncoder.matches(request.getPassword(), foundProfessional.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));

        CustomUserPrincipal principal = new CustomUserPrincipal(
                professional.getFullName(),
                PROFESSIONAL_ROLE,
                professional.getFullName(),
                professional.getId()
        );

        return buildLoginResponse(principal);
    }

    @Override
    public LoginResponse loginAsGuest() {
        CustomUserPrincipal principal = new CustomUserPrincipal(
                "guest",
                GUEST_ROLE,
                GUEST_DISPLAY_NAME,
                null
        );

        return buildLoginResponse(principal);
    }

    @Override
    public AuthMeResponse getAuthenticatedUser(CustomUserPrincipal principal) {
        return AuthMeResponse.builder()
                .role(principal.getRole())
                .displayName(principal.getDisplayName())
                .professionalId(principal.getProfessionalId())
                .build();
    }

    private LoginResponse buildLoginResponse(CustomUserPrincipal principal) {
        return LoginResponse.builder()
                .accessToken(jwtService.generateAccessToken(principal))
                .refreshToken(jwtService.generateRefreshToken(principal))
                .tokenType("Bearer")
                .role(principal.getRole())
                .displayName(principal.getDisplayName())
                .professionalId(principal.getProfessionalId())
                .build();
    }

    @Override
    public LoginResponse refresh(String refreshToken) {
        CustomUserPrincipal principal = jwtService.extractRefreshPrincipal(refreshToken);
        return buildLoginResponse(principal);
    }
}
