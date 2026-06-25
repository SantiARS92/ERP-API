package es.supplesoftware.autonomos_api.expenses;

import es.supplesoftware.autonomos_api.categories.infrastructure.CategoryEntity;
import es.supplesoftware.autonomos_api.expenses.domain.enums.ExpenseCategory;

import es.supplesoftware.autonomos_api.expenses.infrastructure.ExpenseEntity;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductEntity;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductVariantEntity;
import es.supplesoftware.autonomos_api.categories.infrastructure.CategoryRepository;
import es.supplesoftware.autonomos_api.expenses.infrastructure.ExpenseRepository;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductRepository;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductVariantRepository;
import es.supplesoftware.autonomos_api.expenses.application.dto.CreateExpenseRequest;
import es.supplesoftware.autonomos_api.expenses.application.dto.ExpenseResponse;
import es.supplesoftware.autonomos_api.expenses.application.dto.UpdateExpenseRequest;
import es.supplesoftware.autonomos_api.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import es.supplesoftware.autonomos_api.expenses.application.ExpenseService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExpenseInventorySyncIntegrationTest {

    private static final LocalDate EXPENSE_DATE = LocalDate.of(2026, 3, 26);
    private static final String MATCHING_PRODUCT_NAME = "Champ\u00fa L'Oreal Prof";

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryProductRepository inventoryProductRepository;

    @Autowired
    private InventoryProductVariantRepository inventoryProductVariantRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Test
    void createSuppliesExpenseShouldIncreaseStockWhenMatchingProductExists() {
        InventoryProductEntity product = saveActiveProduct(MATCHING_PRODUCT_NAME, 15);

        ExpenseResponse expense = expenseService.createExpense(
                buildCreateRequest(MATCHING_PRODUCT_NAME, ExpenseCategory.SUPPLIES, 5)
        );

        assertThat(expense.getId()).isNotNull();
        assertThat(expense.getAmount()).isEqualByComparingTo("60.00");
        assertThat(getStock(product.getId())).isEqualTo(20);
        assertThat(getExpense(expense.getId()).getInventoryProduct()).isNotNull();
        assertThat(getExpense(expense.getId()).getInventoryProduct().getId()).isEqualTo(product.getId());
        assertThat(getExpense(expense.getId()).getAmount()).isEqualByComparingTo("60.00");
    }

    @Test
    void createSuppliesExpenseWithInventoryProductIdShouldDeriveConceptAndIncreaseSimpleStock() {
        InventoryProductEntity product = saveActiveProduct("Acondicionador Reparador", 8);

        ExpenseResponse expense = expenseService.createExpense(
                buildCreateRequest("Proveedor externo", ExpenseCategory.SUPPLIES, 4, product.getId(), null)
        );

        assertThat(expense.getConcept()).isEqualTo(product.getName());
        assertThat(expense.getInventoryProductId()).isEqualTo(product.getId());
        assertThat(expense.getInventoryProductVariantId()).isNull();
        assertThat(expense.getAmount()).isEqualByComparingTo("48.00");
        assertThat(getStock(product.getId())).isEqualTo(12);
    }

    @Test
    void createSuppliesExpenseWithVariantIdShouldDeriveConceptAndIncreaseVariantStock() {
        InventoryProductVariantEntity variant = saveActiveVariant("Camiseta Basica", "Roja / M", 7);

        ExpenseResponse expense = expenseService.createExpense(
                buildCreateRequest("Proveedor externo", ExpenseCategory.SUPPLIES, 5, null, variant.getId())
        );

        assertThat(expense.getConcept()).isEqualTo("Camiseta Basica - Roja / M");
        assertThat(expense.getInventoryProductId()).isEqualTo(variant.getProduct().getId());
        assertThat(expense.getInventoryProductVariantId()).isEqualTo(variant.getId());
        assertThat(expense.getInventoryProductVariantLabel()).isEqualTo("Roja / M");
        assertThat(expense.getAmount()).isEqualByComparingTo("60.00");
        assertThat(getStock(variant.getProduct().getId())).isZero();
        assertThat(getVariantStock(variant.getId())).isEqualTo(12);

        ExpenseEntity persistedExpense = getExpense(expense.getId());
        assertThat(persistedExpense.getInventoryProduct().getId()).isEqualTo(variant.getProduct().getId());
        assertThat(persistedExpense.getInventoryProductVariant().getId()).isEqualTo(variant.getId());
    }

    @Test
    void createSuppliesExpenseWithVariantIdShouldFailWhenProductIdDoesNotMatch() {
        InventoryProductEntity otherProduct = saveActiveProduct("Producto simple", 3);
        InventoryProductVariantEntity variant = saveActiveVariant("Tornillo Galvanizado", "4mm / 20mm", 11);

        assertThatThrownBy(() -> expenseService.createExpense(
                buildCreateRequest(
                        "Proveedor externo",
                        ExpenseCategory.SUPPLIES,
                        5,
                        otherProduct.getId(),
                        variant.getId()
                )
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no coincide");

        assertThat(getStock(otherProduct.getId())).isEqualTo(3);
        assertThat(getVariantStock(variant.getId())).isEqualTo(11);
    }

    @Test
    void createSuppliesExpenseForProductWithVariantsShouldRequireVariantId() {
        InventoryProductVariantEntity variant = saveActiveVariant("Tornillo Galvanizado", "4mm / 20mm", 11);

        assertThatThrownBy(() -> expenseService.createExpense(
                buildCreateRequest(variant.getProduct().getName(), ExpenseCategory.SUPPLIES, 5)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inventoryProductVariantId es obligatorio");

        assertThat(getVariantStock(variant.getId())).isEqualTo(11);
    }

    @Test
    void createServicesOrOtherExpenseShouldNotChangeStock() {
        InventoryProductEntity product = saveActiveProduct(MATCHING_PRODUCT_NAME, 15);

        ExpenseResponse servicesExpense = expenseService.createExpense(
                buildCreateRequest(MATCHING_PRODUCT_NAME, ExpenseCategory.SERVICES, 5)
        );
        ExpenseResponse otherExpense = expenseService.createExpense(
                buildCreateRequest(MATCHING_PRODUCT_NAME, ExpenseCategory.OTHER, 5)
        );

        assertThat(getStock(product.getId())).isEqualTo(15);
        assertThat(getExpense(servicesExpense.getId()).getInventoryProduct()).isNull();
        assertThat(getExpense(otherExpense.getId()).getInventoryProduct()).isNull();
    }

    @Test
    void createNonSuppliesExpenseWithInventoryReferenceShouldFail() {
        InventoryProductEntity product = saveActiveProduct(MATCHING_PRODUCT_NAME, 15);

        assertThatThrownBy(() -> expenseService.createExpense(
                buildCreateRequest(MATCHING_PRODUCT_NAME, ExpenseCategory.OTHER, 5, product.getId(), null)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("solo se pueden informar en gastos SUPPLIES");

        assertThat(getStock(product.getId())).isEqualTo(15);
    }

    @Test
    void createSuppliesExpenseShouldFailWhenProductDoesNotExist() {
        InventoryProductEntity product = saveActiveProduct(MATCHING_PRODUCT_NAME, 15);
        long countBefore = expenseRepository.count();

        assertThatThrownBy(() -> expenseService.createExpense(
                buildCreateRequest("Producto inexistente", ExpenseCategory.SUPPLIES, 5)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("producto activo de inventario");

        assertThat(getStock(product.getId())).isEqualTo(15);
        assertThat(expenseRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void getSuppliesSuggestionsShouldUseActiveInventoryProductNames() {
        saveActiveProduct("Champu Kerastase", 10);
        saveActiveProduct("Champu L'Oreal Prof", 15);
        saveActiveVariant("Champu con variantes", "Roja / M", 7);
        expenseService.createExpense(buildCreateRequest("Proveedor habitual", ExpenseCategory.OTHER, 1));

        List<String> suggestions = expenseService.getConceptSuggestions("champu", ExpenseCategory.SUPPLIES, 5);

        assertThat(suggestions).containsExactly("Champu Kerastase", "Champu L'Oreal Prof");
    }

    @Test
    void getNonSuppliesSuggestionsShouldKeepUsingExpenseHistory() {
        saveActiveProduct(MATCHING_PRODUCT_NAME, 15);
        expenseService.createExpense(buildCreateRequest("Proveedor habitual", ExpenseCategory.OTHER, 1));

        List<String> suggestions = expenseService.getConceptSuggestions("prove", ExpenseCategory.OTHER, 5);

        assertThat(suggestions).containsExactly("Proveedor habitual");
    }

    @Test
    void updateSyncedExpenseShouldAdjustStockWithoutDuplicating() {
        InventoryProductEntity product = saveActiveProduct(MATCHING_PRODUCT_NAME, 15);
        ExpenseResponse createdExpense = expenseService.createExpense(
                buildCreateRequest(MATCHING_PRODUCT_NAME, ExpenseCategory.SUPPLIES, 5)
        );

        ExpenseResponse updatedExpense = expenseService.updateExpense(
                createdExpense.getId(),
                buildUpdateRequest(MATCHING_PRODUCT_NAME, ExpenseCategory.SUPPLIES, 3)
        );

        assertThat(updatedExpense.getQuantity()).isEqualTo(3);
        assertThat(updatedExpense.getAmount()).isEqualByComparingTo("36.00");
        assertThat(getStock(product.getId())).isEqualTo(18);
        assertThat(getExpense(createdExpense.getId()).getInventoryProduct()).isNotNull();
        assertThat(getExpense(createdExpense.getId()).getInventoryProduct().getId()).isEqualTo(product.getId());
        assertThat(getExpense(createdExpense.getId()).getAmount()).isEqualByComparingTo("36.00");
    }

    @Test
    void updateSyncedExpenseFromSimpleProductToVariantShouldRevertPreviousAndApplyNewStock() {
        InventoryProductEntity simpleProduct = saveActiveProduct("Champu Solido", 15);
        InventoryProductVariantEntity variant = saveActiveVariant("Camiseta Basica", "Azul / L", 10);
        ExpenseResponse createdExpense = expenseService.createExpense(
                buildCreateRequest("Proveedor externo", ExpenseCategory.SUPPLIES, 5, simpleProduct.getId(), null)
        );

        ExpenseResponse updatedExpense = expenseService.updateExpense(
                createdExpense.getId(),
                buildUpdateRequest("Proveedor externo", ExpenseCategory.SUPPLIES, 4, null, variant.getId())
        );

        assertThat(updatedExpense.getConcept()).isEqualTo("Camiseta Basica - Azul / L");
        assertThat(updatedExpense.getInventoryProductId()).isEqualTo(variant.getProduct().getId());
        assertThat(updatedExpense.getInventoryProductVariantId()).isEqualTo(variant.getId());
        assertThat(updatedExpense.getAmount()).isEqualByComparingTo("48.00");
        assertThat(getStock(simpleProduct.getId())).isEqualTo(15);
        assertThat(getVariantStock(variant.getId())).isEqualTo(14);
    }

    @Test
    void deleteSyncedExpenseShouldRevertStock() {
        InventoryProductEntity product = saveActiveProduct(MATCHING_PRODUCT_NAME, 15);
        ExpenseResponse createdExpense = expenseService.createExpense(
                buildCreateRequest(MATCHING_PRODUCT_NAME, ExpenseCategory.SUPPLIES, 5)
        );

        expenseService.deleteExpense(createdExpense.getId());

        assertThat(getStock(product.getId())).isEqualTo(15);
        assertThat(expenseRepository.findById(createdExpense.getId())).isEmpty();
    }

    @Test
    void deleteSyncedVariantExpenseShouldRevertVariantStock() {
        InventoryProductVariantEntity variant = saveActiveVariant("Camiseta Basica", "Roja / M", 7);
        ExpenseResponse createdExpense = expenseService.createExpense(
                buildCreateRequest("Proveedor externo", ExpenseCategory.SUPPLIES, 5, null, variant.getId())
        );

        expenseService.deleteExpense(createdExpense.getId());

        assertThat(getVariantStock(variant.getId())).isEqualTo(7);
        assertThat(expenseRepository.findById(createdExpense.getId())).isEmpty();
    }

    private InventoryProductEntity saveActiveProduct(String name, int stock) {
        CategoryEntity category = categoryRepository.saveAndFlush(
                CategoryEntity.builder()
                        .name("Cabello " + name)
                        .color("gold")
                        .icon("scissors")
                        .build()
        );

        return inventoryProductRepository.saveAndFlush(
                InventoryProductEntity.builder()
                        .name(name)
                        .description("Producto de prueba")
                        .category(category)
                        .stock(stock)
                        .minimumStock(10)
                        .sku("TEST-" + stock)
                        .purchasePrice(new BigDecimal("24.00"))
                        .hasVariants(false)
                        .active(true)
                        .build()
        );
    }

    private InventoryProductVariantEntity saveActiveVariant(String productName, String variantLabel, int stock) {
        CategoryEntity category = categoryRepository.saveAndFlush(
                CategoryEntity.builder()
                        .name("Variantes " + productName)
                        .color("blue")
                        .icon("box")
                        .build()
        );

        InventoryProductEntity product = inventoryProductRepository.saveAndFlush(
                InventoryProductEntity.builder()
                        .name(productName)
                        .description("Producto con variantes de prueba")
                        .category(category)
                        .stock(0)
                        .minimumStock(0)
                        .sku(null)
                        .purchasePrice(null)
                        .hasVariants(true)
                        .active(true)
                        .build()
        );

        return inventoryProductVariantRepository.saveAndFlush(
                InventoryProductVariantEntity.builder()
                        .product(product)
                        .variantLabel(variantLabel)
                        .sku("VAR-" + Math.abs((productName + variantLabel).hashCode()))
                        .stock(stock)
                        .minimumStock(2)
                        .purchasePrice(new BigDecimal("12.00"))
                        .active(true)
                        .build()
        );
    }

    private CreateExpenseRequest buildCreateRequest(String concept, ExpenseCategory category, int quantity) {
        return buildCreateRequest(concept, category, quantity, null, null);
    }

    private CreateExpenseRequest buildCreateRequest(
            String concept,
            ExpenseCategory category,
            int quantity,
            Long inventoryProductId,
            Long inventoryProductVariantId
    ) {
        return CreateExpenseRequest.builder()
                .expenseDate(EXPENSE_DATE)
                .concept(concept)
                .category(category)
                .quantity(quantity)
                .amount(new BigDecimal("12.00"))
                .notes("Reposicion")
                .inventoryProductId(inventoryProductId)
                .inventoryProductVariantId(inventoryProductVariantId)
                .build();
    }

    private UpdateExpenseRequest buildUpdateRequest(String concept, ExpenseCategory category, int quantity) {
        return buildUpdateRequest(concept, category, quantity, null, null);
    }

    private UpdateExpenseRequest buildUpdateRequest(
            String concept,
            ExpenseCategory category,
            int quantity,
            Long inventoryProductId,
            Long inventoryProductVariantId
    ) {
        return UpdateExpenseRequest.builder()
                .expenseDate(EXPENSE_DATE)
                .concept(concept)
                .category(category)
                .quantity(quantity)
                .amount(new BigDecimal("12.00"))
                .notes("Reposicion")
                .inventoryProductId(inventoryProductId)
                .inventoryProductVariantId(inventoryProductVariantId)
                .build();
    }

    private int getStock(Long productId) {
        return inventoryProductRepository.findById(productId)
                .orElseThrow()
                .getStock();
    }

    private int getVariantStock(Long variantId) {
        return inventoryProductVariantRepository.findById(variantId)
                .orElseThrow()
                .getStock();
    }

    private ExpenseEntity getExpense(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow();
    }
}
