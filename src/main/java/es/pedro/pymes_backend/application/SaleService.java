package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.AppointmentEntity;
import es.pedro.pymes_backend.infrastructure.web.dto.PagedResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.CreateSaleRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.SaleResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.UpdateSaleRequest;

import java.time.LocalDate;
import java.util.List;

public interface SaleService {

    SaleResponse createManualSale(CreateSaleRequest request);

    SaleResponse updateSale(Long id, UpdateSaleRequest request);

    void deleteSale(Long id);

    SaleResponse getSaleById(Long id);

    PagedResponse<SaleResponse> getSales(
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    );

    List<SaleResponse> getRecentSales(int days, int limit);

    void createSaleFromConfirmedAppointment(AppointmentEntity appointment);
}