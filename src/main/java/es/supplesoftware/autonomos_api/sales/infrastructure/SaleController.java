package es.supplesoftware.autonomos_api.sales.infrastructure;
import es.supplesoftware.autonomos_api.sales.application.SaleService;
import es.supplesoftware.autonomos_api.sales.infrastructure.SalePdfService;
import es.supplesoftware.autonomos_api.common.dto.PagedResponse;
import es.supplesoftware.autonomos_api.sales.application.dto.CreateSaleRequest;
import es.supplesoftware.autonomos_api.sales.application.dto.SaleResponse;
import es.supplesoftware.autonomos_api.sales.application.dto.UpdateSaleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;
    private final SalePdfService salePdfService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse createSale(@Valid @RequestBody CreateSaleRequest request) {
        return saleService.createManualSale(request);
    }

    @GetMapping
    public PagedResponse<SaleResponse> getSales(
            @RequestParam(required = false) String search,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return saleService.getSales(search, fromDate, toDate, page, size);
    }

    @GetMapping("/recent")
    public List<SaleResponse> getRecentSales(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return saleService.getRecentSales(days, limit);
    }

    @GetMapping("/{id}")
    public SaleResponse getSaleById(@PathVariable Long id) {
        return saleService.getSaleById(id);
    }

    @PutMapping("/{id}")
    public SaleResponse updateSale(@PathVariable Long id, @Valid @RequestBody UpdateSaleRequest request) {
        return saleService.updateSale(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSale(@PathVariable Long id) {
        saleService.deleteSale(id);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportSalesPdf(
            @RequestParam(required = false) String search,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        byte[] pdfBytes = salePdfService.generateSalesPdf(search, fromDate, toDate);

        String fileName = buildPdfFileName(fromDate, toDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private String buildPdfFileName(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null) {
            return "sales-report-" + fromDate + "-to-" + toDate + ".pdf";
        }
        return "sales-report.pdf";
    }
}
