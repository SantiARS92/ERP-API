package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.CategoryEntity;
import es.pedro.pymes_backend.domain.ExpenseCategory;
import es.pedro.pymes_backend.domain.ExpenseEntity;
import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.infrastructure.repository.CategoryRepository;
import es.pedro.pymes_backend.infrastructure.repository.ExpenseRepository;
import es.pedro.pymes_backend.infrastructure.repository.InventoryProductRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateExpenseRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.ExpenseResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.UpdateExpenseRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

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
    private ExpenseRepository expenseRepository;

    @Test
    void createSuppliesExpenseShouldIncreaseStockWhenMatchingProductExists() {
        InventoryProductEntity product = saveActiveProduct(MATCHING_PRODUCT_NAME, 15);

        ExpenseResponse expense = expenseService.createExpense(
                buildCreateRequest(MATCHING_PRODUCT_NAME, ExpenseCategory.SUPPLIES, 5)
        );

        assertThat(expense.getId()).isNotNull();
        assertThat(getStock(product.getId())).isEqualTo(20);
        assertThat(getExpense(expense.getId()).getInventoryProduct()).isNotNull();
        assertThat(getExpense(expense.getId()).getInventoryProduct().getId()).isEqualTo(product.getId());
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
    void createSuppliesExpenseShouldNotChangeStockWhenProductDoesNotExist() {
        InventoryProductEntity product = saveActiveProduct(MATCHING_PRODUCT_NAME, 15);

        ExpenseResponse expense = expenseService.createExpense(
                buildCreateRequest("Producto inexistente", ExpenseCategory.SUPPLIES, 5)
        );

        assertThat(expense.getId()).isNotNull();
        assertThat(getStock(product.getId())).isEqualTo(15);
        assertThat(getExpense(expense.getId()).getInventoryProduct()).isNull();
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
        assertThat(getStock(product.getId())).isEqualTo(18);
        assertThat(getExpense(createdExpense.getId()).getInventoryProduct()).isNotNull();
        assertThat(getExpense(createdExpense.getId()).getInventoryProduct().getId()).isEqualTo(product.getId());
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

    private InventoryProductEntity saveActiveProduct(String name, int stock) {
        CategoryEntity category = categoryRepository.saveAndFlush(
                CategoryEntity.builder()
                        .name("Cabello")
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
                        .active(true)
                        .build()
        );
    }

    private CreateExpenseRequest buildCreateRequest(String concept, ExpenseCategory category, int quantity) {
        return CreateExpenseRequest.builder()
                .expenseDate(EXPENSE_DATE)
                .concept(concept)
                .category(category)
                .quantity(quantity)
                .amount(new BigDecimal("120.00"))
                .notes("Reposicion")
                .build();
    }

    private UpdateExpenseRequest buildUpdateRequest(String concept, ExpenseCategory category, int quantity) {
        return UpdateExpenseRequest.builder()
                .expenseDate(EXPENSE_DATE)
                .concept(concept)
                .category(category)
                .quantity(quantity)
                .amount(new BigDecimal("120.00"))
                .notes("Reposicion")
                .build();
    }

    private int getStock(Long productId) {
        return inventoryProductRepository.findById(productId)
                .orElseThrow()
                .getStock();
    }

    private ExpenseEntity getExpense(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow();
    }
}
