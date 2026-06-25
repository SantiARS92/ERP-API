package es.supplesoftware.autonomos_api.sales.application;
import es.supplesoftware.autonomos_api.appointments.infrastructure.AppointmentEntity;
import es.supplesoftware.autonomos_api.common.dto.PagedResponse;
import es.supplesoftware.autonomos_api.sales.application.dto.CreateSaleRequest;
import es.supplesoftware.autonomos_api.sales.application.dto.SaleResponse;
import es.supplesoftware.autonomos_api.sales.application.dto.UpdateSaleRequest;

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

    void deleteSaleFromAppointment(Long appointmentId);
}
