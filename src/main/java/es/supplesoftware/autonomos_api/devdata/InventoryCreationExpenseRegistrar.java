package es.supplesoftware.autonomos_api.devdata;

import es.supplesoftware.autonomos_api.expenses.domain.enums.ExpenseCategory;

import es.supplesoftware.autonomos_api.expenses.infrastructure.ExpenseEntity;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductEntity;
import es.supplesoftware.autonomos_api.products.infrastructure.InventoryProductVariantEntity;
import es.supplesoftware.autonomos_api.expenses.infrastructure.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class InventoryCreationExpenseRegistrar {

    public static final String AUTOMATIC_INVENTORY_CREATION_NOTE =
            "Gasto generado automaticamente al crear producto en inventario";

    private final ExpenseRepository expenseRepository;

    public void registerInitialPurchaseExpense(InventoryProductEntity product) {
        ExpenseEntity expense = ExpenseEntity.builder()
                .expenseDate(LocalDate.now())
                .concept(product.getName())
                .category(ExpenseCategory.SUPPLIES)
                .quantity(product.getStock())
                .amount(calculateAmount(product))
                .notes(AUTOMATIC_INVENTORY_CREATION_NOTE)
                .inventoryProduct(product)
                .stockMovementApplied(false)
                .build();

        expenseRepository.save(expense);
    }

    public void registerInitialVariantPurchaseExpense(InventoryProductVariantEntity variant) {
        ExpenseEntity expense = ExpenseEntity.builder()
                .expenseDate(LocalDate.now())
                .concept(buildVariantConcept(variant))
                .category(ExpenseCategory.SUPPLIES)
                .quantity(variant.getStock())
                .amount(calculateAmount(variant))
                .notes(AUTOMATIC_INVENTORY_CREATION_NOTE)
                .inventoryProduct(variant.getProduct())
                .inventoryProductVariant(variant)
                .stockMovementApplied(false)
                .build();

        expenseRepository.save(expense);
    }

    private BigDecimal calculateAmount(InventoryProductEntity product) {
        return product.getPurchasePrice().multiply(BigDecimal.valueOf(product.getStock()));
    }

    private BigDecimal calculateAmount(InventoryProductVariantEntity variant) {
        return variant.getPurchasePrice().multiply(BigDecimal.valueOf(variant.getStock()));
    }

    private String buildVariantConcept(InventoryProductVariantEntity variant) {
        return variant.getProduct().getName() + " - " + variant.getVariantLabel();
    }
}
