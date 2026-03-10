package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.CategoryEntity;
import es.pedro.pymes_backend.infrastructure.repository.CategoryRepository;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.CategoryRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.CategoryResponse;
import es.pedro.pymes_backend.infrastructure.web.exception.ConflictException;
import es.pedro.pymes_backend.infrastructure.web.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;

    public CategoryService(CategoryRepository categoryRepository, ServiceRepository serviceRepository) {
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
    }

    public List<CategoryResponse> getAll(String search) {
        List<CategoryEntity> categories;

        if (search != null && !search.isBlank()) {
            categories = categoryRepository.findByNameContainingIgnoreCase(search);
        } else {
            categories = categoryRepository.findAll();
        }

        return categories.stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse getById(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));

        return toResponse(category);
    }

    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException("Ya existe una categoría con ese nombre");
        }

        CategoryEntity category = new CategoryEntity();
        mapRequestToEntity(request, category);

        CategoryEntity saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));

        categoryRepository.findByNameIgnoreCase(request.getName())
                .ifPresent(existingCategory -> {
                    if (!existingCategory.getId().equals(id)) {
                        throw new ConflictException("Ya existe una categoría con ese nombre");
                    }
                });

        mapRequestToEntity(request, category);

        CategoryEntity updated = categoryRepository.save(category);
        return toResponse(updated);
    }

    public void delete(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));

        if (serviceRepository.existsByCategoryId(id)) {
            throw new ConflictException("No se puede eliminar la categoría porque tiene servicios asociados");
        }

        categoryRepository.delete(category);
    }

    private void mapRequestToEntity(CategoryRequest request, CategoryEntity category) {
        category.setName(request.getName());
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());
    }

    private CategoryResponse toResponse(CategoryEntity category) {
        long servicesCount = serviceRepository.countByCategoryId(category.getId());

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .color(category.getColor())
                .icon(category.getIcon())
                .servicesCount(servicesCount)
                .build();
    }
}