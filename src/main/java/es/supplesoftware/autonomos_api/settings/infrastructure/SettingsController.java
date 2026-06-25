package es.supplesoftware.autonomos_api.settings.infrastructure;
import es.supplesoftware.autonomos_api.settings.application.SettingsService;
import es.supplesoftware.autonomos_api.settings.application.dto.BusinessSettingsRequest;
import es.supplesoftware.autonomos_api.settings.application.dto.BusinessSettingsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public BusinessSettingsResponse getSettings() {
        return settingsService.getSettings();
    }

    @PutMapping
    public BusinessSettingsResponse updateSettings(@Valid @RequestBody BusinessSettingsRequest request) {
        return settingsService.updateSettings(request);
    }
}