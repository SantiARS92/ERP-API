package es.supplesoftware.autonomos_api.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class AuthenticatedApiIntegrationTestSupport {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired
    protected WebApplicationContext webApplicationContext;

    protected MockMvc buildBaseMockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    protected MockMvc buildAuthenticatedMockMvc() throws Exception {
        String token = issueAdminToken();

        return MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .defaultRequest(get("/").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .build();
    }

    protected String issueAdminToken() throws Exception {
        return issueLoginToken("admin", "1234");
    }

    protected String issueLoginToken(String username, String password) throws Exception {
        MvcResult result = buildBaseMockMvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        return extractToken(result.getResponse().getContentAsString());
    }

    protected String issueGuestToken() throws Exception {
        MvcResult result = buildBaseMockMvc().perform(post("/api/auth/guest"))
                .andExpect(status().isOk())
                .andReturn();

        return extractToken(result.getResponse().getContentAsString());
    }

    private String extractToken(String responseBody) {
        Matcher matcher = TOKEN_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            throw new IllegalStateException("No se pudo extraer el token: " + responseBody);
        }
        return matcher.group(1);
    }
}
