package es.supplesoftware.autonomos_api.products.application;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductEntity;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductVariantEntity;
import es.supplesoftware.autonomos_api.products.application.dto.CreateInventoryProductRequest;
import es.supplesoftware.autonomos_api.products.application.dto.InventoryProductResponse;
import es.supplesoftware.autonomos_api.common.dto.PagedResponse;
import es.supplesoftware.autonomos_api.products.application.dto.UpdateInventoryProductRequest;

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
