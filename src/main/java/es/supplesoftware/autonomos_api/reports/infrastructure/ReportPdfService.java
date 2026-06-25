package es.supplesoftware.autonomos_api.reports.infrastructure;
public interface ReportPdfService {

    byte[] generateMonthlyDetailedPdf(int year, int month);

    byte[] generateQuarterlyDetailedPdf(int year, int quarter);

    byte[] generateAnnualConsolidatedPdf(int year);
}