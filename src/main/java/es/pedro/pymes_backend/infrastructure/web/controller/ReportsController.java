package es.pedro.pymes_backend.infrastructure.web.controller;

import es.pedro.pymes_backend.application.ReportPdfService;
import es.pedro.pymes_backend.application.ReportsService;
import es.pedro.pymes_backend.infrastructure.web.dto.AnnualConsolidatedReportResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.MonthlyDetailedReportResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.MonthlySimpleReportResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.QuarterlyDetailedReportResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.RankingReportResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.ReportRankingType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportsController {

    private final ReportsService reportsService;
    private final ReportPdfService reportPdfService;

    @GetMapping("/monthly/simple")
    public MonthlySimpleReportResponse getMonthlySimpleReport(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return reportsService.getMonthlySimpleReport(year, month);
    }

    @GetMapping("/monthly/detailed")
    public MonthlyDetailedReportResponse getMonthlyDetailedReport(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return reportsService.getMonthlyDetailedReport(year, month);
    }

    @GetMapping(value = "/monthly/detailed/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getMonthlyDetailedPdf(
            @RequestParam int year,
            @RequestParam int month
    ) {
        byte[] pdf = reportPdfService.generateMonthlyDetailedPdf(year, month);
        String fileName = String.format("informe-mensual-detallado-%d-%02d.pdf", year, month);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/quarterly/detailed")
    public QuarterlyDetailedReportResponse getQuarterlyDetailedReport(
            @RequestParam int year,
            @RequestParam int quarter
    ) {
        return reportsService.getQuarterlyDetailedReport(year, quarter);
    }

    @GetMapping(value = "/quarterly/detailed/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getQuarterlyDetailedPdf(
            @RequestParam int year,
            @RequestParam int quarter
    ) {
        byte[] pdf = reportPdfService.generateQuarterlyDetailedPdf(year, quarter);
        String fileName = String.format("informe-trimestral-detallado-%d-Q%d.pdf", year, quarter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/annual/consolidated")
    public AnnualConsolidatedReportResponse getAnnualConsolidatedReport(
            @RequestParam int year
    ) {
        return reportsService.getAnnualConsolidatedReport(year);
    }

    @GetMapping(value = "/annual/consolidated/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getAnnualConsolidatedPdf(
            @RequestParam int year
    ) {
        byte[] pdf = reportPdfService.generateAnnualConsolidatedPdf(year);
        String fileName = String.format("informe-anual-consolidado-%d.pdf", year);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/ranking")
    public RankingReportResponse getRanking(
            @RequestParam ReportRankingType type,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return reportsService.getRanking(type, year, month, page, size);
    }
}