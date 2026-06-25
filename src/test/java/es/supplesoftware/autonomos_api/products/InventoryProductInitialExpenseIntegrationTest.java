package es.supplesoftware.autonomos_api.products;

import es.supplesoftware.autonomos_api.categories.infrastructure.CategoryEntity;
import es.supplesoftware.autonomos_api.devdata.InventoryCreationExpenseRegistrar;
import es.supplesoftware.autonomos_api.expenses.domain.enums.ExpenseCategory;

import es.supplesoftware.autonomos_api.expenses.infrastructure.ExpenseEntity;
import es.supplesoftware.autonomos_api.expenses.application.ExpenseService;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductEntity;
import es.supplesoftware.autonomos_api.categories.infrastructure.CategoryRepository;
import es.supplesoftware.autonomos_api.expenses.infrastructure.ExpenseRepository;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductRepository;
import es.supplesoftware.autonomos_api.products.application.dto.CreateInventoryProductRequest;
import es.supplesoftware.autonomos_api.products.application.dto.InventoryProductResponse;
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
import es.supplesoftware.autonomos_api.products.application.InventoryProductService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InventoryProductInitialExpenseIntegrationTest {

    @Autowired
    private InventoryProductService inventoryProductService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryProductRepository inventoryProductRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Test
    void createProductWithInitialStockShouldCreateAutomaticExpenseWithoutDuplicatingStock() {
        String productName = "Champu Inicial";

        InventoryProductResponse response = inventoryProductService.createProduct(
                buildCreateRequest(productName, 15, new BigDecimal("18.50"))
        );

        InventoryProductEntity product = inventoryProductRepository.findById(response.getId()).orElseThrow();
        ExpenseEntity expense = getSingleExpenseByConcept(productName);

        assertThat(product.getStock()).isEqualTo(15);
        assertThat(expense.getExpenseDate()).isEqualTo(LocalDate.now());
        assertThat(expense.getCategory()).isEqualTo(ExpenseCategory.SUPPLIES);
        assertThat(expense.getQuantity()).isEqualTo(15);
        assertThat(expense.getAmount()).isEqualByComparingTo("277.50");
        assertThat(expense.getNotes()).isEqualTo(InventoryCreationExpenseRegistrar.AUTOMATIC_INVENTORY_CREATION_NOTE);
        assertThat(expense.getInventoryProduct()).isNotNull();
        assertThat(expense.getInventoryProduct().getId()).isEqualTo(product.getId());
        assertThat(expense.getStockMovementApplied()).isFalse();
    }

    @Test
    void createProductWithZeroStockShouldNotCreateAutomaticExpense() {
        String productName = "Champu Sin Stock";

        InventoryProductResponse response = inventoryProductService.createProduct(
                buildCreateRequest(productName, 0, null)
        );

        InventoryProductEntity product = inventoryProductRepository.findById(response.getId()).orElseThrow();

        assertThat(product.getStock()).isZero();
        assertThat(findExpensesByConcept(productName)).isEmpty();
    }

    @Test
    void createProductWithInitialStockAndInvalidPurchasePriceShouldFail() {
        long productCountBefore = inventoryProductRepository.count();
        long expenseCountBefore = expenseRepository.count();

        assertThatThrownBy(() -> inventoryProductService.createProduct(
                buildCreateRequest("Champu Invalido", 5, null)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("precio de compra");

        assertThat(inventoryProductRepository.count()).isEqualTo(productCountBefore);
        assertThat(expenseRepository.count()).isEqualTo(expenseCountBefore);
    }

    @Test
    void updateAndDeleteAutomaticExpenseShouldNotChangeProductStock() {
        String productName = "Champu Editable";

        InventoryProductResponse response = inventoryProductService.createProduct(
                buildCreateRequest(productName, 12, new BigDecimal("10.00"))
        );

        ExpenseEntity expense = getSingleExpenseByConcept(productName);

        expenseService.updateExpense(
                expense.getId(),
                UpdateExpenseRequest.builder()
                        .expenseDate(expense.getExpenseDate())
                        .concept(productName)
                        .category(ExpenseCategory.SUPPLIES)
                        .quantity(12)
                        .amount(new BigDecimal("130.00"))
                        .notes("Ajuste contable")
                        .build()
        );

        assertThat(getProductStock(response.getId())).isEqualTo(12);
        assertThat(expenseRepository.findById(expense.getId()).orElseThrow().getStockMovementApplied()).isFalse();

        expenseService.deleteExpense(expense.getId());

        assertThat(getProductStock(response.getId())).isEqualTo(12);
        assertThat(expenseRepository.findById(expense.getId())).isEmpty();
    }

    private CreateInventoryProductRequest buildCreateRequest(String name, int stock, BigDecimal purchasePrice) {
        return CreateInventoryProductRequest.builder()
                .name(name)
                .description("Producto de prueba")
                .categoryId(saveCategory(name).getId())
                .stock(stock)
                .minimumStock(3)
                .sku("SKU-" + name.replace(" ", "-"))
                .purchasePrice(purchasePrice)
                .build();
    }

    private CategoryEntity saveCategory(String suffix) {
        return categoryRepository.saveAndFlush(
                CategoryEntity.builder()
                        .name("Categoria " + suffix)
                        .color("gold")
                        .icon("scissors")
                        .build()
        );
    }

    private ExpenseEntity getSingleExpenseByConcept(String concept) {
        List<ExpenseEntity> expenses = findExpensesByConcept(concept);
        assertThat(expenses).hasSize(1);
        return expenses.getFirst();
    }

    private List<ExpenseEntity> findExpensesByConcept(String concept) {
        return expenseRepository.findAll().stream()
                .filter(expense -> concept.equals(expense.getConcept()))
                .toList();
    }

    private int getProductStock(Long productId) {
        return inventoryProductRepository.findById(productId)
                .orElseThrow()
                .getStock();
    }
}
