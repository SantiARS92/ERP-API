package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.infrastructure.web.dto.CreateInventoryProductRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.InventoryProductResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.PagedResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.UpdateInventoryProductRequest;

import java.util.List;

public interface InventoryProductService {

    InventoryProductResponse createProduct(CreateInventoryProductRequest request);

    InventoryProductResponse updateProduct(Long id, UpdateInventoryProductRequest request);

    InventoryProductResponse getProductById(Long id);

    PagedResponse<InventoryProductResponse> getProducts(String search, List<Long> categoryIds, int page, int size);

    void deleteProduct(Long id);

    byte[] exportProductsPdf(String search, List<Long> categoryIds);
}