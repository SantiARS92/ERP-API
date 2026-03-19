package es.pedro.pymes_backend.infrastructure.web.controller;

import es.pedro.pymes_backend.application.SaleService;
import es.pedro.pymes_backend.infrastructure.web.dto.PagedResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateSaleRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.SaleResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.UpdateSaleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

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
}