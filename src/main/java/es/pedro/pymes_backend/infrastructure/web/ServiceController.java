package es.pedro.pymes_backend.infrastructure.web;

import es.pedro.pymes_backend.domain.Service;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "http://localhost:4200")
public class ServiceController {

    private final ServiceRepository repository;

    public ServiceController(ServiceRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Service> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public Service create(@RequestBody Service service) {
        return repository.save(service);
    }

    @PutMapping("/{id}")
    public Service update(@PathVariable Long id, @RequestBody Service service) {
        service.setId(id);
        return repository.save(service);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}