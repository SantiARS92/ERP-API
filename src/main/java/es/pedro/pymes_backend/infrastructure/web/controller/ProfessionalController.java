package es.pedro.pymes_backend.infrastructure.web.controller;

import es.pedro.pymes_backend.application.ProfessionalService;
import es.pedro.pymes_backend.infrastructure.web.dto.ProfessionalRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.ProfessionalResponse;
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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        professionalService.delete(id);
    }
}