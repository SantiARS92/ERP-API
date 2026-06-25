package es.supplesoftware.autonomos_api.expenses.application;
import es.supplesoftware.autonomos_api.expenses.domain.enums.ExpenseCategory;

import es.supplesoftware.autonomos_api.expenses.application.dto.CreateExpenseRequest;
import es.supplesoftware.autonomos_api.expenses.application.dto.ExpenseResponse;
import es.supplesoftware.autonomos_api.common.dto.PagedResponse;
import es.supplesoftware.autonomos_api.expenses.application.dto.UpdateExpenseRequest;

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
