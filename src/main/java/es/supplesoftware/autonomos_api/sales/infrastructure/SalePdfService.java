package es.supplesoftware.autonomos_api.sales.infrastructure;
import java.time.LocalDate;

public interface SalePdfService {
    byte[] generateSalesPdf(String search, LocalDate fromDate, LocalDate toDate);
}