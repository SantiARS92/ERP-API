package es.supplesoftware.autonomos_api.expenses.infrastructure;

import java.time.LocalDate;
import java.util.List;

public interface ExpensePdfService {
    byte[] generateExpensePdf(List<ExpenseEntity> expenses, LocalDate fromDate, LocalDate toDate, String search);
}