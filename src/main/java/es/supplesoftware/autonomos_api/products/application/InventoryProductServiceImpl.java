package es.supplesoftware.autonomos_api.products.application;
import es.supplesoftware.autonomos_api.categories.infrastructure.CategoryEntity;
import es.supplesoftware.autonomos_api.devdata.InventoryCreationExpenseRegistrar;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductEntity;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductVariantEntity;
import es.supplesoftware.autonomos_api.products.domain.enums.InventoryStockStatus;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryPdfService;
import es.supplesoftware.autonomos_api.categories.infrastructure.CategoryRepository;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductRepository;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductVariantRepository;
import es.supplesoftware.autonomos_api.products.application.dto.CreateInventoryProductRequest;
import es.supplesoftware.autonomos_api.products.application.dto.CreateInventoryProductVariantRequest;
import es.supplesoftware.autonomos_api.products.application.dto.InventoryProductResponse;
import es.supplesoftware.autonomos_api.products.application.dto.InventoryProductVariantResponse;
import es.supplesoftware.autonomos_api.common.dto.PagedResponse;
import es.supplesoftware.autonomos_api.products.application.dto.UpdateInventoryProductRequest;
import es.supplesoftware.autonomos_api.products.application.dto.UpdateInventoryProductVariantRequest;
import es.supplesoftware.autonomos_api.common.exception.BadRequestException;
import es.supplesoftware.autonomos_api.common.exception.ConflictException;
import es.supplesoftware.autonomos_api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductSpecification;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryProductServiceImpl implements InventoryProductService {

    private final InventoryProductRepository inventoryProductRepository;
    private final InventoryProductVariantRepository inventoryProductVariantRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryPdfService inventoryPdfService;
    private final InventoryCreationExpenseRegistrar inventoryCreationExpenseRegistrar;

    @Override
    public InventoryProductResponse createProduct(CreateInventoryProductRequest request) {
        boolean hasVariants = Boolean.TRUE.equals(request.getHasVariants());
        validateUniqueNameForCreate(request.getName());

        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con id: " + request.getCategoryId()));

        if (hasVariants) {
            validateCreateVariants(request.getVariants());

            InventoryProductEntity product = InventoryProductEntity.builder()
                    .name(request.getName().trim())
                    .description(normalizeText(request.getDescription()))
                    .category(category)
                    .stock(0)
                    .minimumStock(0)
                    .sku(null)
                    .purchasePrice(null)
                    .hasVariants(true)
                    .active(true)
                    .build();

            InventoryProductEntity saved = inventoryProductRepository.save(product);
            saved.setVariants(createVariants(saved, request.getVariants()));
            return mapToResponse(saved, true);
        }

        validateSimpleProductRequest(request.getStock(), request.getMinimumStock(), request.getPurchasePrice());
        validateUniqueProductSkuForCreate(request.getSku());

        InventoryProductEntity product = InventoryProductEntity.builder()
                .name(request.getName().trim())
                .description(normalizeText(request.getDescription()))
                .category(category)
                .stock(request.getStock())
                .minimumStock(request.getMinimumStock())
                .sku(normalizeText(request.getSku()))
                .purchasePrice(request.getPurchasePrice())
                .hasVariants(false)
                .active(true)
                .build();

        InventoryProductEntity saved = inventoryProductRepository.save(product);
        registerInitialExpenseIfNeeded(saved);
        return mapToResponse(saved, true);
    }

    @Override
    public InventoryProductResponse updateProduct(Long id, UpdateInventoryProductRequest request) {
        InventoryProductEntity product = getActiveProductEntityById(id);
        boolean hasVariants = request.getHasVariants() != null
                ? request.getHasVariants()
                : Boolean.TRUE.equals(product.getHasVariants());

        validateUniqueNameForUpdate(request.getName(), id);

        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con id: " + request.getCategoryId()));

        if (hasVariants) {
            validateSimpleToVariantsTransition(product);
            validateUpdateVariants(request.getVariants(), product.getId());

            product.setName(request.getName().trim());
            product.setDescription(normalizeText(request.getDescription()));
            product.setCategory(category);
            product.setStock(0);
            product.setMinimumStock(0);
            product.setSku(null);
            product.setPurchasePrice(null);
            product.setHasVariants(true);

            InventoryProductEntity updated = inventoryProductRepository.save(product);
            syncVariants(updated, request.getVariants());
            return mapToResponse(updated, true);
        }

        validateVariantsToSimpleTransition(product);
        validateSimpleProductRequest(request.getStock(), request.getMinimumStock(), request.getPurchasePrice());
        validateUniqueProductSkuForUpdate(request.getSku(), id);

        product.setName(request.getName().trim());
        product.setDescription(normalizeText(request.getDescription()));
        product.setCategory(category);
        product.setStock(request.getStock());
        product.setMinimumStock(request.getMinimumStock());
        product.setSku(normalizeText(request.getSku()));
        product.setPurchasePrice(request.getPurchasePrice());
        product.setHasVariants(false);

        deactivateActiveVariants(product);

        InventoryProductEntity updated = inventoryProductRepository.save(product);
        return mapToResponse(updated, true);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryProductResponse getProductById(Long id) {
        return mapToResponse(getActiveProductEntityById(id), true);
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
    public InventoryProductVariantEntity getActiveProductVariantEntityById(Long id) {
        InventoryProductVariantEntity variant = inventoryProductVariantRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Variante de inventario no encontrada con id: " + id));

        if (!Boolean.TRUE.equals(variant.getProduct().getActive())) {
            throw new ResourceNotFoundException("Variante de inventario no encontrada con id: " + id);
        }

        return variant;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getActiveProductNameSuggestions(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        validatePositiveLimit(limit);
        return inventoryProductRepository.findActiveNameSuggestions(query.trim(), PageRequest.of(0, limit));
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
                .map(product -> mapToResponse(product, false))
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
        validateSimpleStockOperation(managedProduct);
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
        validateSimpleStockOperation(managedProduct);
        if (managedProduct.getStock() < quantity) {
            throw new ConflictException("No hay stock suficiente para el producto con id: " + managedProduct.getId());
        }

        managedProduct.setStock(managedProduct.getStock() - quantity);
        return inventoryProductRepository.save(managedProduct);
    }

    @Override
    @Transactional
    public InventoryProductVariantEntity addVariantStock(Long id, int quantity) {
        return addVariantStock(getActiveProductVariantEntityById(id), quantity);
    }

    @Override
    @Transactional
    public InventoryProductVariantEntity addVariantStock(InventoryProductVariantEntity variant, int quantity) {
        validatePositiveQuantity(quantity, "sumar");

        InventoryProductVariantEntity managedVariant = requireVariant(variant);
        managedVariant.setStock(managedVariant.getStock() + quantity);

        return inventoryProductVariantRepository.save(managedVariant);
    }

    @Override
    @Transactional
    public InventoryProductVariantEntity subtractVariantStock(Long id, int quantity) {
        return subtractVariantStock(getActiveProductVariantEntityById(id), quantity);
    }

    @Override
    @Transactional
    public InventoryProductVariantEntity subtractVariantStock(InventoryProductVariantEntity variant, int quantity) {
        validatePositiveQuantity(quantity, "restar");

        InventoryProductVariantEntity managedVariant = requireVariant(variant);
        if (managedVariant.getStock() < quantity) {
            throw new ConflictException("No hay stock suficiente para la variante con id: " + managedVariant.getId());
        }

        managedVariant.setStock(managedVariant.getStock() - quantity);
        return inventoryProductVariantRepository.save(managedVariant);
    }

    @Override
    public void deleteProduct(Long id) {
        InventoryProductEntity product = getActiveProductEntityById(id);

        product.setActive(false);
        deactivateActiveVariants(product);
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

    private List<InventoryProductVariantEntity> createVariants(
            InventoryProductEntity product,
            List<CreateInventoryProductVariantRequest> requests
    ) {
        List<InventoryProductVariantEntity> variants = new ArrayList<>();

        for (CreateInventoryProductVariantRequest request : requests) {
            InventoryProductVariantEntity variant = InventoryProductVariantEntity.builder()
                    .product(product)
                    .variantLabel(request.getVariantLabel().trim())
                    .sku(normalizeText(request.getSku()))
                    .stock(request.getStock())
                    .minimumStock(request.getMinimumStock())
                    .purchasePrice(request.getPurchasePrice())
                    .attributes(normalizeAttributes(request.getAttributes()))
                    .active(true)
                    .build();

            InventoryProductVariantEntity saved = inventoryProductVariantRepository.save(variant);
            registerInitialVariantExpenseIfNeeded(saved);
            variants.add(saved);
        }

        return variants;
    }

    private void syncVariants(InventoryProductEntity product, List<UpdateInventoryProductVariantRequest> requests) {
        List<InventoryProductVariantEntity> existingVariants =
                inventoryProductVariantRepository.findByProductIdOrderByVariantLabelAscIdAsc(product.getId());

        Map<Long, InventoryProductVariantEntity> existingById = existingVariants.stream()
                .filter(variant -> variant.getId() != null)
                .collect(Collectors.toMap(InventoryProductVariantEntity::getId, variant -> variant));

        Set<Long> retainedIds = new HashSet<>();
        List<InventoryProductVariantEntity> newVariants = new ArrayList<>();

        for (UpdateInventoryProductVariantRequest request : requests) {
            InventoryProductVariantEntity variant;
            boolean isNewVariant = request.getId() == null;

            if (isNewVariant) {
                variant = InventoryProductVariantEntity.builder()
                        .product(product)
                        .active(true)
                        .build();
            } else {
                variant = existingById.get(request.getId());
                if (variant == null) {
                    throw new ResourceNotFoundException("Variante de inventario no encontrada con id: " + request.getId());
                }
                retainedIds.add(variant.getId());
            }

            applyVariantData(variant, request);

            InventoryProductVariantEntity saved = inventoryProductVariantRepository.save(variant);
            if (isNewVariant) {
                newVariants.add(saved);
            }
        }

        for (InventoryProductVariantEntity existingVariant : existingVariants) {
            if (Boolean.TRUE.equals(existingVariant.getActive()) && !retainedIds.contains(existingVariant.getId())) {
                existingVariant.setActive(false);
                inventoryProductVariantRepository.save(existingVariant);
            }
        }

        newVariants.forEach(this::registerInitialVariantExpenseIfNeeded);
    }

    private void applyVariantData(
            InventoryProductVariantEntity variant,
            UpdateInventoryProductVariantRequest request
    ) {
        variant.setVariantLabel(request.getVariantLabel().trim());
        variant.setSku(normalizeText(request.getSku()));
        variant.setStock(request.getStock());
        variant.setMinimumStock(request.getMinimumStock());
        variant.setPurchasePrice(request.getPurchasePrice());
        variant.setAttributes(normalizeAttributes(request.getAttributes()));
        variant.setActive(true);
    }

    private void validateCreateVariants(List<CreateInventoryProductVariantRequest> variants) {
        if (CollectionUtils.isEmpty(variants)) {
            throw new BadRequestException("Los productos con variantes deben incluir al menos una variante");
        }

        Set<String> labels = new HashSet<>();
        Set<String> skus = new HashSet<>();

        for (CreateInventoryProductVariantRequest variant : variants) {
            validateVariantFields(
                    variant.getVariantLabel(),
                    variant.getSku(),
                    variant.getStock(),
                    variant.getMinimumStock(),
                    variant.getPurchasePrice(),
                    variant.getAttributes(),
                    labels,
                    skus
            );
            validateUniqueVariantSkuForCreate(variant.getSku());
        }
    }

    private void validateUpdateVariants(List<UpdateInventoryProductVariantRequest> variants, Long productId) {
        if (CollectionUtils.isEmpty(variants)) {
            throw new BadRequestException("Los productos con variantes deben incluir al menos una variante");
        }

        Set<Long> ids = new HashSet<>();
        Set<String> labels = new HashSet<>();
        Set<String> skus = new HashSet<>();

        for (UpdateInventoryProductVariantRequest variant : variants) {
            if (variant.getId() != null && !ids.add(variant.getId())) {
                throw new BadRequestException("No se puede repetir la misma variante en la actualizacion");
            }

            validateVariantFields(
                    variant.getVariantLabel(),
                    variant.getSku(),
                    variant.getStock(),
                    variant.getMinimumStock(),
                    variant.getPurchasePrice(),
                    variant.getAttributes(),
                    labels,
                    skus
            );
            validateUniqueVariantSkuForUpdate(variant.getSku(), productId);
        }
    }

    private void validateVariantFields(
            String variantLabel,
            String sku,
            Integer stock,
            Integer minimumStock,
            BigDecimal purchasePrice,
            Map<String, String> attributes,
            Set<String> labels,
            Set<String> skus
    ) {
        if (!StringUtils.hasText(variantLabel)) {
            throw new BadRequestException("La etiqueta de la variante es obligatoria");
        }
        if (variantLabel.trim().length() > 150) {
            throw new BadRequestException("La etiqueta de la variante no puede superar los 150 caracteres");
        }
        if (!labels.add(normalizeForUniqueness(variantLabel))) {
            throw new ConflictException("No se pueden repetir variantes activas con la misma etiqueta");
        }

        if (stock == null || stock < 0) {
            throw new BadRequestException("El stock de la variante no puede ser negativo");
        }
        if (minimumStock == null || minimumStock < 0) {
            throw new BadRequestException("El stock minimo de la variante no puede ser negativo");
        }

        validateInitialStockPurchasePrice(stock, purchasePrice);
        validateAttributeValues(attributes);

        if (StringUtils.hasText(sku) && !skus.add(normalizeForUniqueness(sku))) {
            throw new ConflictException("No se pueden repetir SKUs entre variantes activas");
        }
    }

    private void validateSimpleProductRequest(Integer stock, Integer minimumStock, BigDecimal purchasePrice) {
        if (stock == null) {
            throw new BadRequestException("El stock actual es obligatorio");
        }
        if (stock < 0) {
            throw new BadRequestException("El stock actual no puede ser negativo");
        }
        if (minimumStock == null) {
            throw new BadRequestException("El stock minimo es obligatorio");
        }
        if (minimumStock < 0) {
            throw new BadRequestException("El stock minimo no puede ser negativo");
        }

        validateInitialStockPurchasePrice(stock, purchasePrice);
    }

    private void validateSimpleToVariantsTransition(InventoryProductEntity product) {
        if (!Boolean.TRUE.equals(product.getHasVariants()) && product.getStock() != null && product.getStock() > 0) {
            throw new BadRequestException(
                    "No se puede convertir en producto con variantes un producto simple con stock base mayor que 0"
            );
        }
    }

    private void validateVariantsToSimpleTransition(InventoryProductEntity product) {
        if (!Boolean.TRUE.equals(product.getHasVariants())) {
            return;
        }

        boolean hasVariantStock = inventoryProductVariantRepository
                .findByProductIdAndActiveTrueOrderByVariantLabelAscIdAsc(product.getId())
                .stream()
                .anyMatch(variant -> variant.getStock() != null && variant.getStock() > 0);

        if (hasVariantStock) {
            throw new BadRequestException(
                    "No se puede convertir en producto simple un producto con variantes activas con stock"
            );
        }
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

    private void validateUniqueProductSkuForCreate(String sku) {
        if (!StringUtils.hasText(sku)) {
            return;
        }

        String normalizedSku = sku.trim();
        if (inventoryProductRepository.existsBySkuIgnoreCaseAndActiveTrue(normalizedSku)
                || inventoryProductVariantRepository.existsBySkuIgnoreCaseAndActiveTrue(normalizedSku)) {
            throw new ConflictException("Ya existe un producto o variante activo con ese SKU");
        }
    }

    private void validateUniqueProductSkuForUpdate(String sku, Long productId) {
        if (!StringUtils.hasText(sku)) {
            return;
        }

        String normalizedSku = sku.trim();
        if (inventoryProductRepository.existsBySkuIgnoreCaseAndActiveTrueAndIdNot(normalizedSku, productId)
                || inventoryProductVariantRepository.existsActiveSkuForOtherProduct(normalizedSku, productId)) {
            throw new ConflictException("Ya existe otro producto o variante activo con ese SKU");
        }
    }

    private void validateUniqueVariantSkuForCreate(String sku) {
        if (!StringUtils.hasText(sku)) {
            return;
        }

        String normalizedSku = sku.trim();
        if (inventoryProductRepository.existsBySkuIgnoreCaseAndActiveTrue(normalizedSku)
                || inventoryProductVariantRepository.existsBySkuIgnoreCaseAndActiveTrue(normalizedSku)) {
            throw new ConflictException("Ya existe un producto o variante activo con ese SKU");
        }
    }

    private void validateUniqueVariantSkuForUpdate(String sku, Long productId) {
        if (!StringUtils.hasText(sku)) {
            return;
        }

        String normalizedSku = sku.trim();
        if (inventoryProductRepository.existsBySkuIgnoreCaseAndActiveTrueAndIdNot(normalizedSku, productId)
                || inventoryProductVariantRepository.existsActiveSkuForOtherProduct(normalizedSku, productId)) {
            throw new ConflictException("Ya existe otro producto o variante activo con ese SKU");
        }
    }

    private void validateInitialStockPurchasePrice(Integer stock, BigDecimal purchasePrice) {
        if (stock != null && stock > 0 && (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BadRequestException(
                    "Si el stock inicial es mayor que 0, el precio de compra debe ser mayor que 0 para generar el gasto inicial"
            );
        }
    }

    private void validateAttributeValues(Map<String, String> attributes) {
        if (CollectionUtils.isEmpty(attributes)) {
            return;
        }

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (!StringUtils.hasText(entry.getKey())) {
                throw new BadRequestException("El nombre del atributo de variante es obligatorio");
            }
            if (!StringUtils.hasText(entry.getValue())) {
                throw new BadRequestException("El valor del atributo de variante es obligatorio");
            }
            if (entry.getKey().trim().length() > 100) {
                throw new BadRequestException("El nombre del atributo de variante no puede superar los 100 caracteres");
            }
            if (entry.getValue().trim().length() > 150) {
                throw new BadRequestException("El valor del atributo de variante no puede superar los 150 caracteres");
            }
        }
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Map<String, String> normalizeAttributes(Map<String, String> attributes) {
        if (CollectionUtils.isEmpty(attributes)) {
            return new LinkedHashMap<>();
        }

        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            normalized.put(entry.getKey().trim(), entry.getValue().trim());
        }
        return normalized;
    }

    private String normalizeRequiredName(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("El nombre del producto es obligatorio");
        }
        return value.trim();
    }

    private String normalizeForUniqueness(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePositiveQuantity(int quantity, String operation) {
        if (quantity <= 0) {
            throw new BadRequestException("La cantidad a " + operation + " debe ser mayor que 0");
        }
    }

    private void validatePositiveLimit(int limit) {
        if (limit <= 0) {
            throw new BadRequestException("El limite debe ser mayor que 0");
        }
    }

    private void validateSimpleStockOperation(InventoryProductEntity product) {
        if (Boolean.TRUE.equals(product.getHasVariants())) {
            throw new BadRequestException("El stock de un producto con variantes debe gestionarse desde sus variantes");
        }
    }

    private void registerInitialExpenseIfNeeded(InventoryProductEntity product) {
        if (product.getStock() == null || product.getStock() <= 0) {
            return;
        }

        inventoryCreationExpenseRegistrar.registerInitialPurchaseExpense(product);
    }

    private void registerInitialVariantExpenseIfNeeded(InventoryProductVariantEntity variant) {
        if (variant.getStock() == null || variant.getStock() <= 0) {
            return;
        }

        inventoryCreationExpenseRegistrar.registerInitialVariantPurchaseExpense(variant);
    }

    private InventoryProductEntity requireProduct(InventoryProductEntity product) {
        if (product == null) {
            throw new BadRequestException("El producto de inventario es obligatorio");
        }
        return product;
    }

    private InventoryProductVariantEntity requireVariant(InventoryProductVariantEntity variant) {
        if (variant == null) {
            throw new BadRequestException("La variante de inventario es obligatoria");
        }
        return variant;
    }

    private void deactivateActiveVariants(InventoryProductEntity product) {
        inventoryProductVariantRepository.findByProductIdAndActiveTrueOrderByVariantLabelAscIdAsc(product.getId())
                .forEach(variant -> {
                    variant.setActive(false);
                    inventoryProductVariantRepository.save(variant);
                });
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

    private InventoryProductResponse mapToResponse(InventoryProductEntity product, boolean includeVariants) {
        boolean hasVariants = Boolean.TRUE.equals(product.getHasVariants());
        List<InventoryProductVariantEntity> activeVariants = hasVariants
                ? inventoryProductVariantRepository.findByProductIdAndActiveTrueOrderByVariantLabelAscIdAsc(product.getId())
                : List.of();
        InventoryStockStatus status = getStatus(product, activeVariants);

        return InventoryProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .stock(getStockForResponse(product, activeVariants))
                .minimumStock(getMinimumStockForResponse(product, activeVariants))
                .sku(hasVariants ? null : product.getSku())
                .purchasePrice(hasVariants ? null : product.getPurchasePrice())
                .hasVariants(hasVariants)
                .variantCount(activeVariants.size())
                .variants(includeVariants ? mapVariantsToResponse(activeVariants) : List.of())
                .active(product.getActive())
                .status(status)
                .statusLabel(getStatusLabel(status))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private List<InventoryProductVariantResponse> mapVariantsToResponse(List<InventoryProductVariantEntity> variants) {
        return variants.stream()
                .map(this::mapVariantToResponse)
                .toList();
    }

    private InventoryProductVariantResponse mapVariantToResponse(InventoryProductVariantEntity variant) {
        return InventoryProductVariantResponse.builder()
                .id(variant.getId())
                .variantLabel(variant.getVariantLabel())
                .sku(variant.getSku())
                .stock(variant.getStock())
                .minimumStock(variant.getMinimumStock())
                .purchasePrice(variant.getPurchasePrice())
                .active(variant.getActive())
                .attributes(new LinkedHashMap<>(variant.getAttributes()))
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .build();
    }

    private int getStockForResponse(
            InventoryProductEntity product,
            List<InventoryProductVariantEntity> activeVariants
    ) {
        if (!Boolean.TRUE.equals(product.getHasVariants())) {
            return product.getStock();
        }

        return activeVariants.stream()
                .mapToInt(InventoryProductVariantEntity::getStock)
                .sum();
    }

    private int getMinimumStockForResponse(
            InventoryProductEntity product,
            List<InventoryProductVariantEntity> activeVariants
    ) {
        if (!Boolean.TRUE.equals(product.getHasVariants())) {
            return product.getMinimumStock();
        }

        return activeVariants.stream()
                .mapToInt(InventoryProductVariantEntity::getMinimumStock)
                .sum();
    }

    private InventoryStockStatus getStatus(
            InventoryProductEntity product,
            List<InventoryProductVariantEntity> activeVariants
    ) {
        if (!Boolean.TRUE.equals(product.getHasVariants())) {
            if (product.getStock() == 0) {
                return InventoryStockStatus.OUT_OF_STOCK;
            }
            if (product.getStock() <= product.getMinimumStock()) {
                return InventoryStockStatus.LOW_STOCK;
            }
            return InventoryStockStatus.IN_STOCK;
        }

        int totalStock = activeVariants.stream()
                .mapToInt(InventoryProductVariantEntity::getStock)
                .sum();

        if (totalStock == 0) {
            return InventoryStockStatus.OUT_OF_STOCK;
        }

        boolean hasLowOrOutVariant = activeVariants.stream()
                .anyMatch(variant -> variant.getStock() <= variant.getMinimumStock());

        return hasLowOrOutVariant ? InventoryStockStatus.LOW_STOCK : InventoryStockStatus.IN_STOCK;
    }

    private String getStatusLabel(InventoryStockStatus status) {
        return switch (status) {
            case IN_STOCK -> "En Stock";
            case LOW_STOCK -> "Bajo Stock";
            case OUT_OF_STOCK -> "Agotado";
        };
    }
}
