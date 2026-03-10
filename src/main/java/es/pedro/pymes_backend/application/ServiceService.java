package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.CategoryEntity;
import es.pedro.pymes_backend.domain.ServiceEntity;
import es.pedro.pymes_backend.infrastructure.repository.CategoryRepository;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.CategorySummaryResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.ServiceRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.ServiceResponse;
import es.pedro.pymes_backend.infrastructure.web.exception.ResourceNotFoundException;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@org.springframework.stereotype.Service
public class ServiceService {

    private final ServiceRepository repository;
    private final CategoryRepository categoryRepository;

    public ServiceService(ServiceRepository repository, CategoryRepository categoryRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
    }

    public List<ServiceResponse> getAll(String search, Boolean active, Long categoryId, Double minPrice, Double maxPrice) {
        Specification<ServiceEntity> specification = Specification
                .where(ServiceSpecification.hasSearch(search))
                .and(ServiceSpecification.hasActive(active))
                .and(ServiceSpecification.hasCategoryId(categoryId))
                .and(ServiceSpecification.hasMinPrice(minPrice))
                .and(ServiceSpecification.hasMaxPrice(maxPrice));

        return repository.findAll(specification)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ServiceResponse getById(Long id) {
        ServiceEntity service = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

        return toResponse(service);
    }

    public ServiceResponse create(ServiceRequest request) {
        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + request.getCategoryId()));

        ServiceEntity service = new ServiceEntity();
        mapRequestToEntity(request, service, category);

        ServiceEntity saved = repository.save(service);
        return toResponse(saved);
    }

    public ServiceResponse update(Long id, ServiceRequest request) {
        ServiceEntity service = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + request.getCategoryId()));

        mapRequestToEntity(request, service, category);

        ServiceEntity updated = repository.save(service);
        return toResponse(updated);
    }

    public void delete(Long id) {
        ServiceEntity service = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

        repository.delete(service);
    }

    private void mapRequestToEntity(ServiceRequest request, ServiceEntity service, CategoryEntity category) {
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setDurationMinutes(request.getDurationMinutes());
        service.setPrice(request.getPrice());
        service.setActive(request.getActive());
        service.setCategory(category);
    }

    private ServiceResponse toResponse(ServiceEntity service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .durationMinutes(service.getDurationMinutes())
                .price(service.getPrice())
                .active(service.getActive())
                .category(
                        CategorySummaryResponse.builder()
                                .id(service.getCategory().getId())
                                .name(service.getCategory().getName())
                                .color(service.getCategory().getColor())
                                .icon(service.getCategory().getIcon())
                                .build()
                )
                .build();
    }
}