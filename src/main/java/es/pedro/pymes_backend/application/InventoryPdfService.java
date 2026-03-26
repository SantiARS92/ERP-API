package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.InventoryProductEntity;

import java.util.List;

public interface InventoryPdfService {
    byte[] generateInventoryPdf(List<InventoryProductEntity> products, String categorySummary, String search);
}