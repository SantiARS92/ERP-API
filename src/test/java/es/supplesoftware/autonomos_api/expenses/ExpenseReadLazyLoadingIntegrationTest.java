package es.supplesoftware.autonomos_api.expenses;

import es.supplesoftware.autonomos_api.categories.infrastructure.CategoryEntity;
import es.supplesoftware.autonomos_api.expenses.domain.enums.ExpenseCategory;

import es.supplesoftware.autonomos_api.categories.infrastructure.CategoryRepository;
import es.supplesoftware.autonomos_api.products.application.InventoryProductService;
import es.supplesoftware.autonomos_api.expenses.application.dto.CreateExpenseRequest;
import es.supplesoftware.autonomos_api.products.application.dto.CreateInventoryProductRequest;
import es.supplesoftware.autonomos_api.products.application.dto.CreateInventoryProductVariantRequest;
import es.supplesoftware.autonomos_api.expenses.application.dto.ExpenseResponse;
import es.supplesoftware.autonomos_api.products.application.dto.InventoryProductResponse;
import es.supplesoftware.autonomos_api.products.application.dto.InventoryProductVariantResponse;
import es.supplesoftware.autonomos_api.common.dto.PagedResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import es.supplesoftware.autonomos_api.expenses.application.ExpenseService;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ExpenseReadLazyLoadingIntegrationTest {

    private static final LocalDate EXPENSE_DATE = LocalDate.of(2026, 4, 23);

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private InventoryProductService inventoryProductService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void getExpensesShouldMapVariantExpenseOutsideTransaction() {
        SeededVariant seededVariant = createVariantProduct("Listado");
        ExpenseResponse created = createVariantExpense(seededVariant);

        PagedResponse<ExpenseResponse> page = assertDoesNotThrow(
                () -> expenseService.getExpenses(null, null, seededVariant.product().getName(), 0, 10)
        );

        assertThat(page.getContent())
                .anySatisfy(response -> assertVariantExpenseResponse(response, created, seededVariant));
    }

    @Test
    void getExpensesShouldMapVariantExpenseWhenSearchIsNullOutsideTransaction() {
        SeededVariant seededVariant = createVariantProduct("ListadoSinBusqueda");
        ExpenseResponse created = createVariantExpense(seededVariant);

        PagedResponse<ExpenseResponse> page = assertDoesNotThrow(
                () -> expenseService.getExpenses(null, null, null, 0, 10)
        );

        assertThat(page.getContent())
                .anySatisfy(response -> assertVariantExpenseResponse(response, created, seededVariant));
    }

    @Test
    void getExpensesShouldMapVariantExpenseWhenSearchIsBlankOutsideTransaction() {
        SeededVariant seededVariant = createVariantProduct("ListadoBusquedaVacia");
        ExpenseResponse created = createVariantExpense(seededVariant);

        PagedResponse<ExpenseResponse> page = assertDoesNotThrow(
                () -> expenseService.getExpenses(null, null, "   ", 0, 10)
        );

        assertThat(page.getContent())
                .anySatisfy(response -> assertVariantExpenseResponse(response, created, seededVariant));
    }

    @Test
    void getExpenseByIdShouldMapVariantExpenseOutsideTransaction() {
        SeededVariant seededVariant = createVariantProduct("Detalle");
        ExpenseResponse created = createVariantExpense(seededVariant);

        ExpenseResponse response = assertDoesNotThrow(() -> expenseService.getExpenseById(created.getId()));

        assertVariantExpenseResponse(response, created, seededVariant);
    }

    @Test
    void getExpensesShouldKeepMappingSimpleProductExpenseOutsideTransaction() {
        InventoryProductResponse product = createSimpleProduct("Simple");
        ExpenseResponse created = expenseService.createExpense(
                CreateExpenseRequest.builder()
                        .expenseDate(EXPENSE_DATE)
                        .concept("Proveedor simple")
                        .category(ExpenseCategory.SUPPLIES)
                        .quantity(2)
                        .amount(new BigDecimal("15.90"))
                        .inventoryProductId(product.getId())
                        .build()
        );

        PagedResponse<ExpenseResponse> page = assertDoesNotThrow(
                () -> expenseService.getExpenses(null, null, product.getName(), 0, 10)
        );

        assertThat(page.getContent())
                .anySatisfy(response -> {
                    assertThat(response.getId()).isEqualTo(created.getId());
                    assertThat(response.getInventoryProductId()).isEqualTo(product.getId());
                    assertThat(response.getInventoryProductVariantId()).isNull();
                    assertThat(response.getInventoryProductVariantLabel()).isNull();
                    assertThat(response.getInventoryProductVariantSku()).isNull();
                    assertThat(response.getAmount()).isEqualByComparingTo("31.80");
                });
    }

    private ExpenseResponse createVariantExpense(SeededVariant seededVariant) {
        return expenseService.createExpense(
                CreateExpenseRequest.builder()
                        .expenseDate(EXPENSE_DATE)
                        .concept("Proveedor variante")
                        .category(ExpenseCategory.SUPPLIES)
                        .quantity(3)
                        .amount(new BigDecimal("12.00"))
                        .inventoryProductId(seededVariant.product().getId())
                        .inventoryProductVariantId(seededVariant.variant().getId())
                        .build()
        );
    }

    private void assertVariantExpenseResponse(
            ExpenseResponse response,
            ExpenseResponse created,
            SeededVariant seededVariant
    ) {
        assertThat(response.getId()).isEqualTo(created.getId());
        assertThat(response.getConcept()).isEqualTo(seededVariant.product().getName() + " - Roja / M");
        assertThat(response.getInventoryProductId()).isEqualTo(seededVariant.product().getId());
        assertThat(response.getInventoryProductVariantId()).isEqualTo(seededVariant.variant().getId());
        assertThat(response.getInventoryProductVariantLabel()).isEqualTo("Roja / M");
        assertThat(response.getInventoryProductVariantSku()).isEqualTo(seededVariant.variant().getSku());
        assertThat(response.getAmount()).isEqualByComparingTo("36.00");
    }

    private InventoryProductResponse createSimpleProduct(String prefix) {
        String suffix = uniqueSuffix();
        CategoryEntity category = saveCategory(prefix + " Simple " + suffix);

        return inventoryProductService.createProduct(
                CreateInventoryProductRequest.builder()
                        .name(prefix + " Producto Simple " + suffix)
                        .description("Producto simple para lectura de gastos")
                        .categoryId(category.getId())
                        .stock(10)
                        .minimumStock(2)
                        .sku("SIMPLE-" + suffix)
                        .purchasePrice(new BigDecimal("15.90"))
                        .hasVariants(false)
                        .build()
        );
    }

    private SeededVariant createVariantProduct(String prefix) {
        String suffix = uniqueSuffix();
        CategoryEntity category = saveCategory(prefix + " Variante " + suffix);
        InventoryProductResponse product = inventoryProductService.createProduct(
                CreateInventoryProductRequest.builder()
                        .name(prefix + " Producto Variante " + suffix)
                        .description("Producto con variante para lectura de gastos")
                        .categoryId(category.getId())
                        .hasVariants(true)
                        .variants(List.of(
                                CreateInventoryProductVariantRequest.builder()
                                        .variantLabel("Roja / M")
                                        .sku("VAR-" + suffix)
                                        .stock(8)
                                        .minimumStock(2)
                                        .purchasePrice(new BigDecimal("12.00"))
                                        .attributes(Map.of("color", "Roja", "talla", "M"))
                                        .build()
                        ))
                        .build()
        );

        return new SeededVariant(product, product.getVariants().get(0));
    }

    private CategoryEntity saveCategory(String name) {
        return categoryRepository.saveAndFlush(
                CategoryEntity.builder()
                        .name(name)
                        .color("blue")
                        .icon("box")
                        .build()
        );
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record SeededVariant(
            InventoryProductResponse product,
            InventoryProductVariantResponse variant
    ) {
    }
}
