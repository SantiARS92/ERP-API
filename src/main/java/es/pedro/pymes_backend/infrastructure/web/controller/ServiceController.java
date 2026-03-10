package es.pedro.pymes_backend.infrastructure.web.controller;

import es.pedro.pymes_backend.application.ServiceService;
import es.pedro.pymes_backend.infrastructure.web.dto.ServiceRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.ServiceResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "http://localhost:4200")
public class ServiceController {

    private final ServiceService serviceService;

    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @GetMapping
    public List<ServiceResponse> getAll() {
        return serviceService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse create(@Valid @RequestBody ServiceRequest request) {
        return serviceService.create(request);
    }

    @PutMapping("/{id}")
    public ServiceResponse update(@PathVariable Long id, @Valid @RequestBody ServiceRequest request) {
        return serviceService.update(id, request);
    }

    @GetMapping("/{id}")
    public ServiceResponse getById(@PathVariable Long id) {
        return serviceService.getById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        serviceService.delete(id);
    }
}