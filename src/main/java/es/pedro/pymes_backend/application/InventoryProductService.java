package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.domain.InventoryProductVariantEntity;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateInventoryProductRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.InventoryProductResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.PagedResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.UpdateInventoryProductRequest;

import java.util.List;

public interface InventoryProductService {

    InventoryProductResponse createProduct(CreateInventoryProductRequest request);

    InventoryProductResponse updateProduct(Long id, UpdateInventoryProductRequest request);

    InventoryProductResponse getProductById(Long id);

    InventoryProductEntity getActiveProductEntityById(Long id);

    InventoryProductEntity getActiveProductEntityByName(String name);

    InventoryProductVariantEntity getActiveProductVariantEntityById(Long id);

    List<String> getActiveProductNameSuggestions(String query, int limit);

    PagedResponse<InventoryProductResponse> getProducts(String search, List<Long> categoryIds, int page, int size);

    InventoryProductEntity addStock(Long id, int quantity);

    InventoryProductEntity addStock(InventoryProductEntity product, int quantity);

    InventoryProductEntity subtractStock(Long id, int quantity);

    InventoryProductEntity subtractStock(InventoryProductEntity product, int quantity);

    InventoryProductVariantEntity addVariantStock(Long id, int quantity);

    InventoryProductVariantEntity addVariantStock(InventoryProductVariantEntity variant, int quantity);

    InventoryProductVariantEntity subtractVariantStock(Long id, int quantity);

    InventoryProductVariantEntity subtractVariantStock(InventoryProductVariantEntity variant, int quantity);

    void deleteProduct(Long id);

    byte[] exportProductsPdf(String search, List<Long> categoryIds);
}
