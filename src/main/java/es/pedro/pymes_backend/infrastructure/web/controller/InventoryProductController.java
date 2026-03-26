package es.pedro.pymes_backend.infrastructure.web.controller;

import es.pedro.pymes_backend.application.InventoryProductService;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateInventoryProductRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.InventoryProductResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.PagedResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.UpdateInventoryProductRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/products")
@RequiredArgsConstructor
@Validated
public class InventoryProductController {

    private final InventoryProductService inventoryProductService;

    @PostMapping
    public ResponseEntity<InventoryProductResponse> createProduct(
            @Valid @RequestBody CreateInventoryProductRequest request
    ) {
        return ResponseEntity.ok(inventoryProductService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInventoryProductRequest request
    ) {
        return ResponseEntity.ok(inventoryProductService.updateProduct(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryProductService.getProductById(id));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<InventoryProductResponse>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size
    ) {
        return ResponseEntity.ok(inventoryProductService.getProducts(search, categoryIds, page, size));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        inventoryProductService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportProductsPdf(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<Long> categoryIds
    ) {
        byte[] pdf = inventoryProductService.exportProductsPdf(search, categoryIds);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("inventory-report.pdf")
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}