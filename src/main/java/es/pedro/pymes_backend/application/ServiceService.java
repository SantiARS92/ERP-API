package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.ServiceEntity;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.ServiceRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.ServiceResponse;
import es.pedro.pymes_backend.infrastructure.web.exception.ResourceNotFoundException;

import java.util.List;

@org.springframework.stereotype.Service
public class ServiceService {

    private final ServiceRepository repository;

    public ServiceService(ServiceRepository repository) {
        this.repository = repository;
    }

    public List<ServiceResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ServiceResponse create(ServiceRequest request) {
        ServiceEntity serviceEntity = new ServiceEntity();
        mapRequestToEntity(request, serviceEntity);

        ServiceEntity saved = repository.save(serviceEntity);
        return toResponse(saved);
    }

    public ServiceResponse update(Long id, ServiceRequest request) {
        ServiceEntity serviceEntity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

        mapRequestToEntity(request, serviceEntity);

        ServiceEntity updated = repository.save(serviceEntity);
        return toResponse(updated);
    }

    public void delete(Long id) {
        ServiceEntity serviceEntity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

        repository.delete(serviceEntity);
    }

    public ServiceResponse getById(Long id) {
        ServiceEntity service = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

        return toResponse(service);
    }

    private void mapRequestToEntity(ServiceRequest request, ServiceEntity serviceEntity) {
        serviceEntity.setName(request.getName());
        serviceEntity.setDescription(request.getDescription());
        serviceEntity.setDurationMinutes(request.getDurationMinutes());
        serviceEntity.setPrice(request.getPrice());
        serviceEntity.setActive(request.getActive());
    }

    private ServiceResponse toResponse(ServiceEntity serviceEntity) {
        return ServiceResponse.builder()
                .id(serviceEntity.getId())
                .name(serviceEntity.getName())
                .description(serviceEntity.getDescription())
                .durationMinutes(serviceEntity.getDurationMinutes())
                .price(serviceEntity.getPrice())
                .active(serviceEntity.getActive())
                .build();
    }
}