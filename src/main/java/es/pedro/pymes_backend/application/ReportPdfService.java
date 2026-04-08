package es.pedro.pymes_backend.application;

public interface ReportPdfService {

    byte[] generateMonthlyDetailedPdf(int year, int month);

    byte[] generateQuarterlyDetailedPdf(int year, int quarter);

    byte[] generateAnnualConsolidatedPdf(int year);
}