package es.pedro.pymes_backend.infrastructure.web.controller;

import es.pedro.pymes_backend.application.SettingsService;
import es.pedro.pymes_backend.infrastructure.web.dto.BusinessSettingsRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.BusinessSettingsResponse;
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