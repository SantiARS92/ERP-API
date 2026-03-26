package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.ExpenseCategory;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateExpenseRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.ExpenseResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.PagedResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.UpdateExpenseRequest;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseService {

    ExpenseResponse createExpense(CreateExpenseRequest request);

    ExpenseResponse updateExpense(Long id, UpdateExpenseRequest request);

    ExpenseResponse getExpenseById(Long id);

    PagedResponse<ExpenseResponse> getExpenses(LocalDate fromDate, LocalDate toDate, String search, int page, int size);

    List<ExpenseResponse> getRecentExpenses(int limit);

    List<String> getConceptSuggestions(String query, ExpenseCategory category, int limit);

    void deleteExpense(Long id);

    byte[] exportExpensesPdf(LocalDate fromDate, LocalDate toDate, String search);
}
