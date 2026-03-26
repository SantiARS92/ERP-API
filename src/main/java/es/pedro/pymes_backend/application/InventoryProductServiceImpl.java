package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.CategoryEntity;
import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.domain.InventoryStockStatus;
import es.pedro.pymes_backend.infrastructure.repository.CategoryRepository;
import es.pedro.pymes_backend.infrastructure.repository.InventoryProductRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateInventoryProductRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.InventoryProductResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.PagedResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.UpdateInventoryProductRequest;
import es.pedro.pymes_backend.infrastructure.web.exception.BadRequestException;
import es.pedro.pymes_backend.infrastructure.web.exception.ConflictException;
import es.pedro.pymes_backend.infrastructure.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryProductServiceImpl implements InventoryProductService {

    private final InventoryProductRepository inventoryProductRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryPdfService inventoryPdfService;
    private final InventoryCreationExpenseRegistrar inventoryCreationExpenseRegistrar;

    @Override
    public InventoryProductResponse createProduct(CreateInventoryProductRequest request) {
        validateUniqueNameForCreate(request.getName());
        validateUniqueSkuForCreate(request.getSku());
        validateInitialStockPurchasePrice(request.getStock(), request.getPurchasePrice());

        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + request.getCategoryId()));

        InventoryProductEntity product = InventoryProductEntity.builder()
                .name(request.getName().trim())
                .description(normalizeText(request.getDescription()))
                .category(category)
                .stock(request.getStock())
                .minimumStock(request.getMinimumStock())
                .sku(normalizeText(request.getSku()))
                .purchasePrice(request.getPurchasePrice())
                .active(true)
                .build();

        InventoryProductEntity saved = inventoryProductRepository.save(product);
        registerInitialExpenseIfNeeded(saved);
        return mapToResponse(saved);
    }

    @Override
    public InventoryProductResponse updateProduct(Long id, UpdateInventoryProductRequest request) {
        InventoryProductEntity product = getActiveProductEntityById(id);

        validateUniqueNameForUpdate(request.getName(), id);
        validateUniqueSkuForUpdate(request.getSku(), id);

        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + request.getCategoryId()));

        product.setName(request.getName().trim());
        product.setDescription(normalizeText(request.getDescription()));
        product.setCategory(category);
        product.setStock(request.getStock());
        product.setMinimumStock(request.getMinimumStock());
        product.setSku(normalizeText(request.getSku()));
        product.setPurchasePrice(request.getPurchasePrice());

        InventoryProductEntity updated = inventoryProductRepository.save(product);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryProductResponse getProductById(Long id) {
        return mapToResponse(getActiveProductEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryProductEntity getActiveProductEntityById(Long id) {
        return inventoryProductRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto de inventario no encontrado con id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryProductEntity getActiveProductEntityByName(String name) {
        String normalizedName = normalizeRequiredName(name);

        return inventoryProductRepository.findByNameIgnoreCaseAndActiveTrue(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto de inventario no encontrado con nombre: " + normalizedName
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getActiveProductNameSuggestions(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        validatePositiveLimit(limit);

        return inventoryProductRepository.findActiveNameSuggestions(
                query.trim(),
                PageRequest.of(0, limit)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<InventoryProductResponse> getProducts(String search, List<Long> categoryIds, int page, int size) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))
        );

        Page<InventoryProductEntity> productPage = inventoryProductRepository.findAll(
                InventoryProductSpecification.filter(search, categoryIds),
                pageable
        );

        List<InventoryProductResponse> content = productPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new PagedResponse<>(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isFirst(),
                productPage.isLast()
        );
    }

    @Override
    @Transactional
    public InventoryProductEntity addStock(Long id, int quantity) {
        return addStock(getActiveProductEntityById(id), quantity);
    }

    @Override
    @Transactional
    public InventoryProductEntity addStock(InventoryProductEntity product, int quantity) {
        validatePositiveQuantity(quantity, "sumar");

        InventoryProductEntity managedProduct = requireProduct(product);
        managedProduct.setStock(managedProduct.getStock() + quantity);

        return inventoryProductRepository.save(managedProduct);
    }

    @Override
    @Transactional
    public InventoryProductEntity subtractStock(Long id, int quantity) {
        return subtractStock(getActiveProductEntityById(id), quantity);
    }

    @Override
    @Transactional
    public InventoryProductEntity subtractStock(InventoryProductEntity product, int quantity) {
        validatePositiveQuantity(quantity, "restar");

        InventoryProductEntity managedProduct = requireProduct(product);
        if (managedProduct.getStock() < quantity) {
            throw new ConflictException("No hay stock suficiente para el producto con id: " + managedProduct.getId());
        }

        managedProduct.setStock(managedProduct.getStock() - quantity);
        return inventoryProductRepository.save(managedProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        InventoryProductEntity product = getActiveProductEntityById(id);

        product.setActive(false);
        inventoryProductRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportProductsPdf(String search, List<Long> categoryIds) {
        List<InventoryProductEntity> products = inventoryProductRepository.findAll(
                InventoryProductSpecification.filter(search, categoryIds),
                Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))
        );

        String categorySummary = buildCategorySummary(categoryIds);

        return inventoryPdfService.generateInventoryPdf(products, categorySummary, search);
    }

    private void validateUniqueNameForCreate(String name) {
        if (inventoryProductRepository.existsByNameIgnoreCaseAndActiveTrue(name.trim())) {
            throw new ConflictException("Ya existe un producto activo con ese nombre");
        }
    }

    private void validateUniqueNameForUpdate(String name, Long id) {
        if (inventoryProductRepository.existsByNameIgnoreCaseAndActiveTrueAndIdNot(name.trim(), id)) {
            throw new ConflictException("Ya existe otro producto activo con ese nombre");
        }
    }

    private void validateUniqueSkuForCreate(String sku) {
        if (StringUtils.hasText(sku) && inventoryProductRepository.existsBySkuIgnoreCaseAndActiveTrue(sku.trim())) {
            throw new ConflictException("Ya existe un producto activo con ese SKU");
        }
    }

    private void validateUniqueSkuForUpdate(String sku, Long id) {
        if (StringUtils.hasText(sku) && inventoryProductRepository.existsBySkuIgnoreCaseAndActiveTrueAndIdNot(sku.trim(), id)) {
            throw new ConflictException("Ya existe otro producto activo con ese SKU");
        }
    }

    private void validateInitialStockPurchasePrice(Integer stock, BigDecimal purchasePrice) {
        if (stock != null && stock > 0 && (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BadRequestException(
                    "Si el stock inicial es mayor que 0, el precio de compra debe ser mayor que 0 para generar el gasto inicial"
            );
        }
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRequiredName(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("El nombre del producto es obligatorio");
        }
        return value.trim();
    }

    private void validatePositiveQuantity(int quantity, String operation) {
        if (quantity <= 0) {
            throw new BadRequestException("La cantidad a " + operation + " debe ser mayor que 0");
        }
    }

    private void validatePositiveLimit(int limit) {
        if (limit <= 0) {
            throw new BadRequestException("El lÃ­mite debe ser mayor que 0");
        }
    }

    private void registerInitialExpenseIfNeeded(InventoryProductEntity product) {
        if (product.getStock() == null || product.getStock() <= 0) {
            return;
        }

        inventoryCreationExpenseRegistrar.registerInitialPurchaseExpense(product);
    }

    private InventoryProductEntity requireProduct(InventoryProductEntity product) {
        if (product == null) {
            throw new BadRequestException("El producto de inventario es obligatorio");
        }
        return product;
    }

    private String buildCategorySummary(List<Long> categoryIds) {
        if (CollectionUtils.isEmpty(categoryIds)) {
            return "Todas";
        }

        List<String> categoryNames = categoryRepository.findAllById(categoryIds)
                .stream()
                .map(CategoryEntity::getName)
                .sorted()
                .collect(Collectors.toList());

        if (categoryNames.isEmpty()) {
            return "Todas";
        }

        return String.join(", ", categoryNames);
    }

    private InventoryProductResponse mapToResponse(InventoryProductEntity product) {
        InventoryStockStatus status = getStatus(product);

        return InventoryProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .stock(product.getStock())
                .minimumStock(product.getMinimumStock())
                .sku(product.getSku())
                .purchasePrice(product.getPurchasePrice())
                .active(product.getActive())
                .status(status)
                .statusLabel(getStatusLabel(status))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private InventoryStockStatus getStatus(InventoryProductEntity product) {
        if (product.getStock() == 0) {
            return InventoryStockStatus.OUT_OF_STOCK;
        }
        if (product.getStock() <= product.getMinimumStock()) {
            return InventoryStockStatus.LOW_STOCK;
        }
        return InventoryStockStatus.IN_STOCK;
    }

    private String getStatusLabel(InventoryStockStatus status) {
        return switch (status) {
            case IN_STOCK -> "En Stock";
            case LOW_STOCK -> "Bajo Stock";
            case OUT_OF_STOCK -> "Agotado";
        };
    }
}
