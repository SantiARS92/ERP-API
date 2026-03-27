package es.pedro.pymes_backend.infrastructure.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SettingsAndProfessionalsApiIntegrationTest {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    private static final String SETTINGS_UPDATE_JSON = """
            {
              "businessName": "Centro de Est\\u00e9tica",
              "phone": "+34 912 345 678",
              "address": "Calle Principal 123, Madrid",
              "contactEmail": "info@centroestetica.com",
              "onlineBookingEnabled": true,
              "defaultAppointmentDurationMinutes": 30,
              "minimumAdvanceMinutes": 120,
              "maximumAdvanceDays": 90,
              "freeCancellationHours": 24,
              "cancellationPolicy": "Cancelaciones gratuitas hasta 24 horas antes.",
              "openingHours": [
                { "dayOfWeek": "MONDAY", "enabled": true,  "openingTime": "09:00", "closingTime": "18:00" },
                { "dayOfWeek": "TUESDAY", "enabled": true, "openingTime": "09:00", "closingTime": "18:00" },
                { "dayOfWeek": "WEDNESDAY", "enabled": true, "openingTime": "09:00", "closingTime": "18:00" },
                { "dayOfWeek": "THURSDAY", "enabled": true, "openingTime": "09:00", "closingTime": "18:00" },
                { "dayOfWeek": "FRIDAY", "enabled": true, "openingTime": "09:00", "closingTime": "19:00" },
                { "dayOfWeek": "SATURDAY", "enabled": true, "openingTime": "10:00", "closingTime": "14:00" },
                { "dayOfWeek": "SUNDAY", "enabled": false, "openingTime": null, "closingTime": null }
              ],
              "specialClosures": [
                { "closureDate": "2026-12-25", "reason": "Navidad" },
                { "closureDate": "2026-01-01", "reason": "A\\u00f1o nuevo" }
              ]
            }
            """;

    private static final String PROFESSIONAL_CREATE_JSON = """
            {
              "fullName": "Ana Garc\\u00eda",
              "specialty": "Esteticista",
              "phone": "+34 600 000 000",
              "email": "ana@negocio.com",
              "active": true
            }
            """;

    private static final String PROFESSIONAL_DEACTIVATE_JSON = """
            {
              "fullName": "Ana Garc\\u00eda",
              "specialty": "Esteticista",
              "phone": "+34 600 000 000",
              "email": "ana@negocio.com",
              "active": false
            }
            """;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void settingsApiShouldReturnDefaultsAndPersistUpdates() throws Exception {
        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value(""))
                .andExpect(jsonPath("$.phone").value(""))
                .andExpect(jsonPath("$.address").value(""))
                .andExpect(jsonPath("$.contactEmail").value(""))
                .andExpect(jsonPath("$.onlineBookingEnabled").value(false))
                .andExpect(jsonPath("$.defaultAppointmentDurationMinutes").value(30))
                .andExpect(jsonPath("$.minimumAdvanceMinutes").value(120))
                .andExpect(jsonPath("$.maximumAdvanceDays").value(90))
                .andExpect(jsonPath("$.freeCancellationHours").value(24))
                .andExpect(jsonPath("$.cancellationPolicy").value(nullValue()))
                .andExpect(jsonPath("$.openingHours", hasSize(7)))
                .andExpect(jsonPath("$.openingHours[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.openingHours[0].enabled").value(false))
                .andExpect(jsonPath("$.openingHours[0].openingTime").value(nullValue()))
                .andExpect(jsonPath("$.openingHours[0].closingTime").value(nullValue()))
                .andExpect(jsonPath("$.openingHours[6].dayOfWeek").value("SUNDAY"))
                .andExpect(jsonPath("$.openingHours[6].enabled").value(false))
                .andExpect(jsonPath("$.specialClosures", hasSize(0)));

        mockMvc.perform(put("/api/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SETTINGS_UPDATE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Centro de Est\u00e9tica"))
                .andExpect(jsonPath("$.phone").value("+34 912 345 678"))
                .andExpect(jsonPath("$.address").value("Calle Principal 123, Madrid"))
                .andExpect(jsonPath("$.contactEmail").value("info@centroestetica.com"))
                .andExpect(jsonPath("$.onlineBookingEnabled").value(true))
                .andExpect(jsonPath("$.defaultAppointmentDurationMinutes").value(30))
                .andExpect(jsonPath("$.minimumAdvanceMinutes").value(120))
                .andExpect(jsonPath("$.maximumAdvanceDays").value(90))
                .andExpect(jsonPath("$.freeCancellationHours").value(24))
                .andExpect(jsonPath("$.cancellationPolicy").value("Cancelaciones gratuitas hasta 24 horas antes."))
                .andExpect(jsonPath("$.openingHours", hasSize(7)))
                .andExpect(jsonPath("$.openingHours[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.openingHours[0].enabled").value(true))
                .andExpect(jsonPath("$.openingHours[0].openingTime").value("09:00"))
                .andExpect(jsonPath("$.openingHours[4].dayOfWeek").value("FRIDAY"))
                .andExpect(jsonPath("$.openingHours[4].closingTime").value("19:00"))
                .andExpect(jsonPath("$.openingHours[6].dayOfWeek").value("SUNDAY"))
                .andExpect(jsonPath("$.openingHours[6].enabled").value(false))
                .andExpect(jsonPath("$.openingHours[6].openingTime").value(nullValue()))
                .andExpect(jsonPath("$.openingHours[6].closingTime").value(nullValue()))
                .andExpect(jsonPath("$.specialClosures", hasSize(2)))
                .andExpect(jsonPath("$.specialClosures[0].closureDate").value("2026-01-01"))
                .andExpect(jsonPath("$.specialClosures[0].reason").value("A\u00f1o nuevo"))
                .andExpect(jsonPath("$.specialClosures[1].closureDate").value("2026-12-25"))
                .andExpect(jsonPath("$.specialClosures[1].reason").value("Navidad"));

        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Centro de Est\u00e9tica"))
                .andExpect(jsonPath("$.phone").value("+34 912 345 678"))
                .andExpect(jsonPath("$.address").value("Calle Principal 123, Madrid"))
                .andExpect(jsonPath("$.contactEmail").value("info@centroestetica.com"))
                .andExpect(jsonPath("$.onlineBookingEnabled").value(true))
                .andExpect(jsonPath("$.defaultAppointmentDurationMinutes").value(30))
                .andExpect(jsonPath("$.minimumAdvanceMinutes").value(120))
                .andExpect(jsonPath("$.maximumAdvanceDays").value(90))
                .andExpect(jsonPath("$.freeCancellationHours").value(24))
                .andExpect(jsonPath("$.cancellationPolicy").value("Cancelaciones gratuitas hasta 24 horas antes."))
                .andExpect(jsonPath("$.openingHours", hasSize(7)))
                .andExpect(jsonPath("$.openingHours[5].dayOfWeek").value("SATURDAY"))
                .andExpect(jsonPath("$.openingHours[5].openingTime").value("10:00"))
                .andExpect(jsonPath("$.openingHours[5].closingTime").value("14:00"))
                .andExpect(jsonPath("$.openingHours[6].dayOfWeek").value("SUNDAY"))
                .andExpect(jsonPath("$.openingHours[6].enabled").value(false))
                .andExpect(jsonPath("$.openingHours[6].openingTime").value(nullValue()))
                .andExpect(jsonPath("$.openingHours[6].closingTime").value(nullValue()))
                .andExpect(jsonPath("$.specialClosures", hasSize(2)))
                .andExpect(jsonPath("$.specialClosures[0].closureDate").value("2026-01-01"))
                .andExpect(jsonPath("$.specialClosures[1].closureDate").value("2026-12-25"));
    }

    @Test
    void professionalsApiShouldCreateRetrieveListAndFilterByActiveStatus() throws Exception {
        Long professionalId = extractId(
                mockMvc.perform(post("/api/professionals")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PROFESSIONAL_CREATE_JSON))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.fullName").value("Ana Garc\u00eda"))
                        .andExpect(jsonPath("$.specialty").value("Esteticista"))
                        .andExpect(jsonPath("$.phone").value("+34 600 000 000"))
                        .andExpect(jsonPath("$.email").value("ana@negocio.com"))
                        .andExpect(jsonPath("$.active").value(true))
                        .andReturn()
        );

        mockMvc.perform(get("/api/professionals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(professionalId.intValue()))
                .andExpect(jsonPath("$[0].fullName").value("Ana Garc\u00eda"))
                .andExpect(jsonPath("$[0].active").value(true));

        mockMvc.perform(get("/api/professionals")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(professionalId.intValue()))
                .andExpect(jsonPath("$[0].active").value(true));

        mockMvc.perform(get("/api/professionals/{id}", professionalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(professionalId.intValue()))
                .andExpect(jsonPath("$.fullName").value("Ana Garc\u00eda"))
                .andExpect(jsonPath("$.specialty").value("Esteticista"))
                .andExpect(jsonPath("$.phone").value("+34 600 000 000"))
                .andExpect(jsonPath("$.email").value("ana@negocio.com"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(put("/api/professionals/{id}", professionalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROFESSIONAL_DEACTIVATE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(professionalId.intValue()))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/professionals")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/professionals")
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(professionalId.intValue()))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    private Long extractId(MvcResult result) throws Exception {
        Matcher matcher = ID_PATTERN.matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new IllegalStateException("No se pudo extraer el id de la respuesta: " + result.getResponse().getContentAsString());
        }
        return Long.parseLong(matcher.group(1));
    }
}
