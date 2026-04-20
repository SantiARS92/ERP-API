package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.CategoryEntity;
import es.pedro.pymes_backend.domain.ExpenseCategory;
import es.pedro.pymes_backend.domain.ExpenseEntity;
import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.domain.InventoryProductVariantEntity;
import es.pedro.pymes_backend.domain.InventoryStockStatus;
import es.pedro.pymes_backend.infrastructure.repository.CategoryRepository;
import es.pedro.pymes_backend.infrastructure.repository.ExpenseRepository;
import es.pedro.pymes_backend.infrastructure.repository.InventoryProductRepository;
import es.pedro.pymes_backend.infrastructure.repository.InventoryProductVariantRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateInventoryProductRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateInventoryProductVariantRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.InventoryProductResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.PagedResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.UpdateInventoryProductRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.UpdateInventoryProductVariantRequest;
import es.pedro.pymes_backend.infrastructure.web.exception.BadRequestException;
import es.pedro.pymes_backend.infrastructure.web.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InventoryProductVariantsIntegrationTest {

    @Autowired
    private InventoryProductService inventoryProductService;

    @Autowired
    private InventoryProductRepository inventoryProductRepository;

    @Autowired
    private InventoryProductVariantRepository inventoryProductVariantRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void createProductWithVariantsShouldAggregateStockAndCreateInitialVariantExpense() {
        CategoryEntity category = saveCategory();

        InventoryProductResponse created = inventoryProductService.createProduct(
                CreateInventoryProductRequest.builder()
                        .name("Camiseta Basica")
                        .description("Producto con variantes")
                        .categoryId(category.getId())
                        .hasVariants(true)
                        .variants(List.of(
                                variant("Roja / M", "CAM-RO-M", 5, 2, new BigDecimal("3.00"), Map.of("color", "Roja", "talla", "M")),
                                variant("Azul / M", "CAM-AZ-M", 0, 1, null, Map.of("color", "Azul", "talla", "M"))
                        ))
                        .build()
        );

        assertThat(created.getHasVariants()).isTrue();
        assertThat(created.getVariantCount()).isEqualTo(2);
        assertThat(created.getStock()).isEqualTo(5);
        assertThat(created.getMinimumStock()).isEqualTo(3);
        assertThat(created.getStatus()).isEqualTo(InventoryStockStatus.LOW_STOCK);
        assertThat(created.getVariants()).hasSize(2);
        assertThat(created.getVariants())
                .filteredOn(variant -> "Roja / M".equals(variant.getVariantLabel()))
                .singleElement()
                .satisfies(variant -> assertThat(variant.getAttributes()).containsEntry("color", "Roja"));

        InventoryProductEntity product = inventoryProductRepository.findById(created.getId()).orElseThrow();
        assertThat(product.getHasVariants()).isTrue();
        assertThat(product.getStock()).isZero();
        assertThat(product.getSku()).isNull();

        ExpenseEntity initialExpense = getSingleExpenseByConcept("Camiseta Basica - Roja / M");
        assertThat(initialExpense.getCategory()).isEqualTo(ExpenseCategory.SUPPLIES);
        assertThat(initialExpense.getQuantity()).isEqualTo(5);
        assertThat(initialExpense.getAmount()).isEqualByComparingTo("15.00");
        assertThat(initialExpense.getStockMovementApplied()).isFalse();
        assertThat(initialExpense.getInventoryProduct()).isNotNull();
        assertThat(initialExpense.getInventoryProduct().getId()).isEqualTo(product.getId());
        assertThat(initialExpense.getInventoryProductVariant()).isNotNull();
        assertThat(initialExpense.getInventoryProductVariant().getVariantLabel()).isEqualTo("Roja / M");

        PagedResponse<InventoryProductResponse> page = inventoryProductService.getProducts("Camiseta", null, 0, 10);
        assertThat(page.getContent()).hasSize(1);
        InventoryProductResponse listed = page.getContent().getFirst();
        assertThat(listed.getHasVariants()).isTrue();
        assertThat(listed.getVariantCount()).isEqualTo(2);
        assertThat(listed.getStock()).isEqualTo(5);
        assertThat(listed.getStatus()).isEqualTo(InventoryStockStatus.LOW_STOCK);
        assertThat(listed.getVariants()).isEmpty();
    }

    @Test
    void updateSimpleProductWithBaseStockToVariantsShouldFail() {
        CategoryEntity category = saveCategory();
        InventoryProductResponse simple = inventoryProductService.createProduct(
                CreateInventoryProductRequest.builder()
                        .name("Champu Simple")
                        .categoryId(category.getId())
                        .stock(3)
                        .minimumStock(1)
                        .sku("CH-SIMPLE-" + UUID.randomUUID())
                        .purchasePrice(new BigDecimal("2.00"))
                        .build()
        );

        assertThatThrownBy(() -> inventoryProductService.updateProduct(
                simple.getId(),
                UpdateInventoryProductRequest.builder()
                        .name("Champu Simple")
                        .categoryId(category.getId())
                        .hasVariants(true)
                        .variants(List.of(updateVariant(null, "500ml", "CH-500", 1, 1, new BigDecimal("2.00"))))
                        .build()
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("stock base mayor que 0");
    }

    @Test
    void createVariantProductShouldRejectSkuUsedBySimpleProduct() {
        CategoryEntity category = saveCategory();
        inventoryProductService.createProduct(
                CreateInventoryProductRequest.builder()
                        .name("Producto Simple SKU")
                        .categoryId(category.getId())
                        .stock(0)
                        .minimumStock(0)
                        .sku("SKU-COMPARTIDO")
                        .purchasePrice(null)
                        .build()
        );

        assertThatThrownBy(() -> inventoryProductService.createProduct(
                CreateInventoryProductRequest.builder()
                        .name("Producto Variante SKU")
                        .categoryId(category.getId())
                        .hasVariants(true)
                        .variants(List.of(variant("Roja / M", "SKU-COMPARTIDO", 0, 0, null, null)))
                        .build()
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("SKU");
    }

    @Test
    void deleteProductShouldDeactivateItsVariants() {
        CategoryEntity category = saveCategory();
        InventoryProductResponse created = inventoryProductService.createProduct(
                CreateInventoryProductRequest.builder()
                        .name("Tornillo Galvanizado")
                        .categoryId(category.getId())
                        .hasVariants(true)
                        .variants(List.of(
                                variant("4mm / 20mm", "TOR-4-20", 0, 1, null, null),
                                variant("4mm / 30mm", "TOR-4-30", 0, 1, null, null)
                        ))
                        .build()
        );

        inventoryProductService.deleteProduct(created.getId());

        assertThat(inventoryProductRepository.findById(created.getId()).orElseThrow().getActive()).isFalse();
        assertThat(inventoryProductVariantRepository.findByProductIdOrderByVariantLabelAscIdAsc(created.getId()))
                .extracting(InventoryProductVariantEntity::getActive)
                .containsOnly(false);
    }

    private CreateInventoryProductVariantRequest variant(
            String label,
            String sku,
            int stock,
            int minimumStock,
            BigDecimal purchasePrice,
            Map<String, String> attributes
    ) {
        return CreateInventoryProductVariantRequest.builder()
                .variantLabel(label)
                .sku(sku)
                .stock(stock)
                .minimumStock(minimumStock)
                .purchasePrice(purchasePrice)
                .attributes(attributes)
                .build();
    }

    private UpdateInventoryProductVariantRequest updateVariant(
            Long id,
            String label,
            String sku,
            int stock,
            int minimumStock,
            BigDecimal purchasePrice
    ) {
        return UpdateInventoryProductVariantRequest.builder()
                .id(id)
                .variantLabel(label)
                .sku(sku)
                .stock(stock)
                .minimumStock(minimumStock)
                .purchasePrice(purchasePrice)
                .build();
    }

    private CategoryEntity saveCategory() {
        return categoryRepository.saveAndFlush(
                CategoryEntity.builder()
                        .name("Categoria " + UUID.randomUUID())
                        .color("blue")
                        .icon("tag")
                        .build()
        );
    }

    private ExpenseEntity getSingleExpenseByConcept(String concept) {
        List<ExpenseEntity> expenses = expenseRepository.findAll().stream()
                .filter(expense -> concept.equals(expense.getConcept()))
                .toList();
        assertThat(expenses).hasSize(1);
        return expenses.getFirst();
    }
}
