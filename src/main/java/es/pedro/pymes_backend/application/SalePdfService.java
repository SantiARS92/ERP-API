package es.pedro.pymes_backend.application;

import java.time.LocalDate;

public interface SalePdfService {
    byte[] generateSalesPdf(String search, LocalDate fromDate, LocalDate toDate);
}