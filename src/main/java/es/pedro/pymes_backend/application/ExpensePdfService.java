package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.ExpenseEntity;

import java.time.LocalDate;
import java.util.List;

public interface ExpensePdfService {
    byte[] generateExpensePdf(List<ExpenseEntity> expenses, LocalDate fromDate, LocalDate toDate, String search);
}