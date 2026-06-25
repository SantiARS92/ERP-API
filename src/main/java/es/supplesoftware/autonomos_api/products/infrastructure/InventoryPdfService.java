package es.supplesoftware.autonomos_api.products.infrastructure;

import java.util.List;

public interface InventoryPdfService {
    byte[] generateInventoryPdf(List<InventoryProductEntity> products, String categorySummary, String search);
}