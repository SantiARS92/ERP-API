package es.pedro.pymes_backend.infrastructure.web;

import es.pedro.pymes_backend.domain.ProfessionalEntity;
import es.pedro.pymes_backend.infrastructure.repository.ProfessionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthApiIntegrationTest extends AuthenticatedApiIntegrationTestSupport {

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildBaseMockMvc();
    }

    @Test
    void loginShouldAuthenticateFixedAdminOnly() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.displayName").value("Administrador"))
                .andExpect(jsonPath("$.professionalId").value(nullValue()));
    }

    @Test
    void loginShouldRejectInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "incorrecta"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Credenciales invalidas"));
    }

    @Test
    void loginShouldAuthenticateActiveProfessionalWithCaseInsensitiveTrimmedFullName() throws Exception {
        ProfessionalEntity professional = saveProfessional("Ana Garcia", "clave-profesional", true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "  aNa gArCiA  ",
                                  "password": "clave-profesional"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("PROFESSIONAL"))
                .andExpect(jsonPath("$.displayName").value("Ana Garcia"))
                .andExpect(jsonPath("$.professionalId").value(professional.getId().intValue()));
    }

    @Test
    void loginShouldRejectProfessionalWithInvalidPassword() throws Exception {
        saveProfessional("Laura Acceso", "clave-correcta", true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "laura acceso",
                                  "password": "clave-incorrecta"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciales invalidas"));
    }

    @Test
    void loginShouldRejectInactiveProfessional() throws Exception {
        saveProfessional("Marta Inactiva", "clave-inactiva", false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "marta inactiva",
                                  "password": "clave-inactiva"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciales invalidas"));
    }

    @Test
    void loginShouldRejectProfessionalWithoutPasswordHash() throws Exception {
        saveProfessionalWithPasswordHash("Pablo Sin Hash", null, true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "pablo sin hash",
                                  "password": "cualquier-clave"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciales invalidas"));
    }

    @Test
    void guestEndpointShouldIssueGuestToken() throws Exception {
        mockMvc.perform(post("/api/auth/guest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("GUEST"))
                .andExpect(jsonPath("$.displayName").value("Invitado"))
                .andExpect(jsonPath("$.professionalId").value(nullValue()));
    }

    @Test
    void guestTokenShouldReturnGuestIdentityThroughMe() throws Exception {
        String token = issueGuestToken();

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("GUEST"))
                .andExpect(jsonPath("$.displayName").value("Invitado"))
                .andExpect(jsonPath("$.professionalId").value(nullValue()));
    }

    @Test
    void meShouldReturnAuthenticatedAdminAndProfessionalData() throws Exception {
        String adminToken = issueAdminToken();

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.displayName").value("Administrador"))
                .andExpect(jsonPath("$.professionalId").value(nullValue()));

        ProfessionalEntity professional = saveProfessional("Clara Perfil", "clave-perfil", true);
        String professionalToken = issueLoginToken("Clara Perfil", "clave-perfil");

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + professionalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PROFESSIONAL"))
                .andExpect(jsonPath("$.displayName").value("Clara Perfil"))
                .andExpect(jsonPath("$.professionalId").value(professional.getId().intValue()));
    }

    @Test
    void protectedEndpointsShouldReturnUnauthorizedWithoutValidToken() throws Exception {
        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        mockMvc.perform(get("/api/settings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Token invalido o expirado"));
    }

    private ProfessionalEntity saveProfessional(String fullName, String rawPassword, boolean active) {
        return saveProfessionalWithPasswordHash(
                fullName,
                rawPassword == null ? null : passwordEncoder.encode(rawPassword),
                active
        );
    }

    private ProfessionalEntity saveProfessionalWithPasswordHash(String fullName, String passwordHash, boolean active) {
        ProfessionalEntity professional = new ProfessionalEntity();
        professional.setFullName(fullName);
        professional.setSpecialty("Estetica");
        professional.setPhone("600000000");
        professional.setEmail(fullName.replace(" ", ".").toLowerCase() + "@test.com");
        professional.setPasswordHash(passwordHash);
        professional.setActive(active);
        return professionalRepository.saveAndFlush(professional);
    }
}
