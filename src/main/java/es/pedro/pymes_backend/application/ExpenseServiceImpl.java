package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.ExpenseCategory;
import es.pedro.pymes_backend.domain.ExpenseEntity;
import es.pedro.pymes_backend.domain.InventoryProductEntity;
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

        syncInventoryIfApplicable(expense);

        ExpenseEntity saved = expenseRepository.save(expense);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ExpenseResponse updateExpense(Long id, UpdateExpenseRequest request) {
        ExpenseEntity expense = getExpenseEntityById(id);
        boolean stockMovementApplied = isStockMovementApplied(expense);

        revertInventorySyncIfNeeded(expense);

        expense.setExpenseDate(request.getExpenseDate());
        expense.setConcept(request.getConcept().trim());
        expense.setCategory(request.getCategory());
        expense.setQuantity(request.getQuantity());
        expense.setAmount(request.getAmount());
        expense.setNotes(normalizeNotes(request.getNotes()));
        expense.setInventoryProduct(null);
        expense.setStockMovementApplied(stockMovementApplied);

        syncInventoryIfApplicable(expense);

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

        /*
         * FUTURO:
         * Si category == SUPPLIES y este gasto impactó inventario,
         * aquí habrá que revertir el stock asociado.
         */
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

    private void syncInventoryIfApplicable(ExpenseEntity expense) {
        expense.setInventoryProduct(null);

        if (expense.getCategory() != ExpenseCategory.SUPPLIES) {
            return;
        }

        InventoryProductEntity inventoryProduct = requireActiveInventoryProductByConcept(expense.getConcept());
        expense.setInventoryProduct(inventoryProduct);

        if (!isStockMovementApplied(expense)) {
            return;
        }

        InventoryProductEntity updatedProduct = inventoryProductService.addStock(inventoryProduct, expense.getQuantity());
        expense.setInventoryProduct(updatedProduct);
    }

    private void revertInventorySyncIfNeeded(ExpenseEntity expense) {
        if (expense.getInventoryProduct() == null || !isStockMovementApplied(expense)) {
            return;
        }

        inventoryProductService.subtractStock(expense.getInventoryProduct(), expense.getQuantity());
        expense.setInventoryProduct(null);
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

    private boolean isStockMovementApplied(ExpenseEntity expense) {
        return !Boolean.FALSE.equals(expense.getStockMovementApplied());
    }

    private ExpenseResponse mapToResponse(ExpenseEntity expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .expenseDate(expense.getExpenseDate())
                .concept(expense.getConcept())
                .category(expense.getCategory())
                .categoryLabel(getCategoryLabel(expense.getCategory()))
                .quantity(expense.getQuantity())
                .amount(expense.getAmount())
                .notes(expense.getNotes())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }

    private String getCategoryLabel(ExpenseCategory category) {
        return switch (category) {
            case SUPPLIES -> "Suministros";
            case SERVICES -> "Servicios";
            case OTHER -> "Otros";
        };
    }
}
