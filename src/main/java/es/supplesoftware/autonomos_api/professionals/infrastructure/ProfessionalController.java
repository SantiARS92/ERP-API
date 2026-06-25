package es.supplesoftware.autonomos_api.professionals.infrastructure;
import es.supplesoftware.autonomos_api.professionals.application.ProfessionalService;
import es.supplesoftware.autonomos_api.professionals.application.dto.ProfessionalRequest;
import es.supplesoftware.autonomos_api.professionals.application.dto.ProfessionalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

    private final ProfessionalService professionalService;

    @GetMapping
    public List<ProfessionalResponse> getAll(@RequestParam(required = false) Boolean active) {
        return professionalService.getAll(active);
    }

    @GetMapping("/{id}")
    public ProfessionalResponse getById(@PathVariable Long id) {
        return professionalService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessionalResponse create(@Valid @RequestBody ProfessionalRequest request) {
        return professionalService.create(request);
    }

    @PutMapping("/{id}")
    public ProfessionalResponse update(@PathVariable Long id, @Valid @RequestBody ProfessionalRequest request) {
        return professionalService.update(id, request);
    }
}