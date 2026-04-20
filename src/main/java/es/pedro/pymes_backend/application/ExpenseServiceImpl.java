package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.ExpenseCategory;
import es.pedro.pymes_backend.domain.ExpenseEntity;
import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.domain.InventoryProductVariantEntity;
import es.pedro.pymes_backend.infrastructure.repository.ExpenseRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateExpenseRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.ExpenseResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.PagedResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.UpdateExpenseRequest;
import es.pedro.pymes_backend.infrastructure.web.exception.BadRequestException;
import es.pedro.pymes_backend.infrastructure.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpensePdfService expensePdfService;
    private final InventoryProductService inventoryProductService;

    @Override
    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request) {
        ExpenseEntity expense = ExpenseEntity.builder()
                .expenseDate(request.getExpenseDate())
                .concept(request.getConcept().trim())
                .category(request.getCategory())
                .quantity(request.getQuantity())
                .amount(request.getAmount())
                .notes(normalizeNotes(request.getNotes()))
                .stockMovementApplied(true)
                .build();

        syncInventoryIfApplicable(
                expense,
                request.getInventoryProductId(),
                request.getInventoryProductVariantId(),
                null,
                null
        );

        ExpenseEntity saved = expenseRepository.save(expense);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ExpenseResponse updateExpense(Long id, UpdateExpenseRequest request) {
        ExpenseEntity expense = getExpenseEntityById(id);
        boolean stockMovementApplied = isStockMovementApplied(expense);
        InventoryProductEntity previousInventoryProduct = expense.getInventoryProduct();
        InventoryProductVariantEntity previousInventoryProductVariant = expense.getInventoryProductVariant();

        revertInventorySyncIfNeeded(expense);

        expense.setExpenseDate(request.getExpenseDate());
        expense.setConcept(request.getConcept().trim());
        expense.setCategory(request.getCategory());
        expense.setQuantity(request.getQuantity());
        expense.setAmount(request.getAmount());
        expense.setNotes(normalizeNotes(request.getNotes()));
        expense.setInventoryProduct(null);
        expense.setInventoryProductVariant(null);
        expense.setStockMovementApplied(stockMovementApplied);

        syncInventoryIfApplicable(
                expense,
                request.getInventoryProductId(),
                request.getInventoryProductVariantId(),
                previousInventoryProduct,
                previousInventoryProductVariant
        );

        ExpenseEntity updated = expenseRepository.save(expense);
        return mapToResponse(updated);
    }

    @Override
    public ExpenseResponse getExpenseById(Long id) {
        return mapToResponse(getExpenseEntityById(id));
    }

    @Override
    public PagedResponse<ExpenseResponse> getExpenses(LocalDate fromDate, LocalDate toDate, String search, int page, int size) {
        validateDateRange(fromDate, toDate);

        Sort sort = Sort.by(
                Sort.Order.desc("expenseDate"),
                Sort.Order.desc("id")
        );

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ExpenseEntity> expensePage = expenseRepository.findAll(
                ExpenseSpecification.filter(fromDate, toDate, search),
                pageable
        );

        List<ExpenseResponse> content = expensePage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new PagedResponse<>(
                content,
                expensePage.getNumber(),
                expensePage.getSize(),
                expensePage.getTotalElements(),
                expensePage.getTotalPages(),
                expensePage.isFirst(),
                expensePage.isLast()
        );
    }

    @Override
    public List<ExpenseResponse> getRecentExpenses(int limit) {
        if (limit < 1) {
            throw new BadRequestException("El límite debe ser mayor que 0");
        }

        Pageable pageable = PageRequest.of(
                0,
                limit,
                Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id"))
        );

        return expenseRepository.findAll(pageable)
                .getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<String> getConceptSuggestions(String query, ExpenseCategory category, int limit) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        if (limit < 1) {
            throw new BadRequestException("El límite debe ser mayor que 0");
        }

        if (category == ExpenseCategory.SUPPLIES) {
            return inventoryProductService.getActiveProductNameSuggestions(query.trim(), limit);
        }

        return expenseRepository.findConceptSuggestions(query.trim(), PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public void deleteExpense(Long id) {
        ExpenseEntity expense = getExpenseEntityById(id);

        revertInventorySyncIfNeeded(expense);

        expenseRepository.delete(expense);
    }

    @Override
    public byte[] exportExpensesPdf(LocalDate fromDate, LocalDate toDate, String search) {
        validateDateRange(fromDate, toDate);

        List<ExpenseEntity> expenses = expenseRepository.findAll(
                ExpenseSpecification.filter(fromDate, toDate, search),
                Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id"))
        );

        return expensePdfService.generateExpensePdf(expenses, fromDate, toDate, search);
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("La fecha desde no puede ser mayor que la fecha hasta");
        }
    }

    private String normalizeNotes(String notes) {
        if (!StringUtils.hasText(notes)) {
            return null;
        }
        return notes.trim();
    }

    private ExpenseEntity getExpenseEntityById(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado con id: " + id));
    }

    private void syncInventoryIfApplicable(
            ExpenseEntity expense,
            Long inventoryProductId,
            Long inventoryProductVariantId,
            InventoryProductEntity previousInventoryProduct,
            InventoryProductVariantEntity previousInventoryProductVariant
    ) {
        expense.setInventoryProduct(null);
        expense.setInventoryProductVariant(null);

        if (expense.getCategory() != ExpenseCategory.SUPPLIES) {
            validateNoInventoryReferencesForNonSupplies(inventoryProductId, inventoryProductVariantId);
            return;
        }

        if (inventoryProductVariantId != null) {
            syncVariantExpense(expense, inventoryProductId, inventoryProductVariantId);
            return;
        }

        if (inventoryProductId != null) {
            syncSimpleProductExpense(expense, inventoryProductService.getActiveProductEntityById(inventoryProductId));
            return;
        }

        if (reattachAutomaticInventoryReference(
                expense,
                previousInventoryProduct,
                previousInventoryProductVariant
        )) {
            return;
        }

        syncSimpleProductExpense(expense, requireActiveInventoryProductByConcept(expense.getConcept()));
    }

    private void syncVariantExpense(ExpenseEntity expense, Long inventoryProductId, Long inventoryProductVariantId) {
        InventoryProductVariantEntity variant = inventoryProductService
                .getActiveProductVariantEntityById(inventoryProductVariantId);
        InventoryProductEntity product = variant.getProduct();

        validateVariantProductMatch(product, inventoryProductId);

        expense.setConcept(buildVariantConcept(variant));
        expense.setInventoryProduct(product);
        expense.setInventoryProductVariant(variant);

        if (!isStockMovementApplied(expense)) {
            return;
        }

        InventoryProductVariantEntity updatedVariant = inventoryProductService.addVariantStock(variant, expense.getQuantity());
        expense.setInventoryProduct(product);
        expense.setInventoryProductVariant(updatedVariant);
    }

    private void syncSimpleProductExpense(ExpenseEntity expense, InventoryProductEntity inventoryProduct) {
        validateSimpleInventoryProduct(inventoryProduct);

        expense.setConcept(inventoryProduct.getName());
        expense.setInventoryProduct(inventoryProduct);
        expense.setInventoryProductVariant(null);

        if (!isStockMovementApplied(expense)) {
            return;
        }

        InventoryProductEntity updatedProduct = inventoryProductService.addStock(inventoryProduct, expense.getQuantity());
        expense.setInventoryProduct(updatedProduct);
    }

    private void revertInventorySyncIfNeeded(ExpenseEntity expense) {
        if (!isStockMovementApplied(expense)) {
            return;
        }

        if (expense.getInventoryProductVariant() != null) {
            inventoryProductService.subtractVariantStock(expense.getInventoryProductVariant(), expense.getQuantity());
            expense.setInventoryProduct(null);
            expense.setInventoryProductVariant(null);
            return;
        }

        if (expense.getInventoryProduct() != null) {
            inventoryProductService.subtractStock(expense.getInventoryProduct(), expense.getQuantity());
            expense.setInventoryProduct(null);
        }
    }

    private InventoryProductEntity requireActiveInventoryProductByConcept(String concept) {
        try {
            return inventoryProductService.getActiveProductEntityByName(concept);
        } catch (ResourceNotFoundException ex) {
            throw new BadRequestException(
                    "Para gastos de suministros, el concepto debe coincidir con un producto activo de inventario"
            );
        }
    }

    private boolean reattachAutomaticInventoryReference(
            ExpenseEntity expense,
            InventoryProductEntity previousInventoryProduct,
            InventoryProductVariantEntity previousInventoryProductVariant
    ) {
        if (isStockMovementApplied(expense)) {
            return false;
        }

        if (previousInventoryProductVariant != null) {
            expense.setConcept(buildVariantConcept(previousInventoryProductVariant));
            expense.setInventoryProduct(previousInventoryProductVariant.getProduct());
            expense.setInventoryProductVariant(previousInventoryProductVariant);
            return true;
        }

        if (previousInventoryProduct != null) {
            expense.setConcept(previousInventoryProduct.getName());
            expense.setInventoryProduct(previousInventoryProduct);
            expense.setInventoryProductVariant(null);
            return true;
        }

        return false;
    }

    private void validateNoInventoryReferencesForNonSupplies(Long inventoryProductId, Long inventoryProductVariantId) {
        if (inventoryProductId != null || inventoryProductVariantId != null) {
            throw new BadRequestException(
                    "inventoryProductId e inventoryProductVariantId solo se pueden informar en gastos SUPPLIES"
            );
        }
    }

    private void validateVariantProductMatch(InventoryProductEntity product, Long inventoryProductId) {
        if (inventoryProductId != null && !product.getId().equals(inventoryProductId)) {
            throw new BadRequestException(
                    "inventoryProductId no coincide con el producto base de inventoryProductVariantId"
            );
        }
    }

    private void validateSimpleInventoryProduct(InventoryProductEntity inventoryProduct) {
        if (Boolean.TRUE.equals(inventoryProduct.getHasVariants())) {
            throw new BadRequestException(
                    "inventoryProductVariantId es obligatorio para gastos SUPPLIES de productos con variantes"
            );
        }
    }

    private String buildVariantConcept(InventoryProductVariantEntity variant) {
        return variant.getProduct().getName() + " - " + variant.getVariantLabel();
    }

    private boolean isStockMovementApplied(ExpenseEntity expense) {
        return !Boolean.FALSE.equals(expense.getStockMovementApplied());
    }

    private ExpenseResponse mapToResponse(ExpenseEntity expense) {
        InventoryProductVariantEntity variant = expense.getInventoryProductVariant();

        return ExpenseResponse.builder()
                .id(expense.getId())
                .expenseDate(expense.getExpenseDate())
                .concept(expense.getConcept())
                .category(expense.getCategory())
                .categoryLabel(getCategoryLabel(expense.getCategory()))
                .quantity(expense.getQuantity())
                .amount(expense.getAmount())
                .notes(expense.getNotes())
                .inventoryProductId(resolveInventoryProductId(expense))
                .inventoryProductVariantId(variant != null ? variant.getId() : null)
                .inventoryProductVariantLabel(variant != null ? variant.getVariantLabel() : null)
                .inventoryProductVariantSku(variant != null ? variant.getSku() : null)
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }

    private Long resolveInventoryProductId(ExpenseEntity expense) {
        if (expense.getInventoryProduct() != null) {
            return expense.getInventoryProduct().getId();
        }

        if (expense.getInventoryProductVariant() != null) {
            return expense.getInventoryProductVariant().getProduct().getId();
        }

        return null;
    }

    private String getCategoryLabel(ExpenseCategory category) {
        return switch (category) {
            case SUPPLIES -> "Suministros";
            case SERVICES -> "Servicios";
            case OTHER -> "Otros";
        };
    }
}
