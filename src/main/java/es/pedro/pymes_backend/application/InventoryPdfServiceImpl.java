package es.pedro.pymes_backend.application;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.domain.InventoryStockStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class InventoryPdfServiceImpl implements InventoryPdfService {

    @Override
    public byte[] generateInventoryPdf(List<InventoryProductEntity> products, String categorySummary, String search) {
        try {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            PdfWriter.getInstance(document, outputStream);
            document.open();

            addHeader(document, categorySummary, search);
            addSummary(document, products);
            addDetailTable(document, products);

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de inventario", e);
        }
    }

    private void addHeader(Document document, String categorySummary, String search) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

        Paragraph title = new Paragraph("Reporte de Inventario", titleFont);
        title.setSpacingAfter(10f);
        document.add(title);

        Paragraph categoryParagraph = new Paragraph("Categorías: " + categorySummary, textFont);
        categoryParagraph.setSpacingAfter(4f);
        document.add(categoryParagraph);

        String searchText = StringUtils.hasText(search) ? search.trim() : "Sin filtro";
        Paragraph searchParagraph = new Paragraph("Búsqueda: " + searchText, textFont);
        searchParagraph.setSpacingAfter(12f);
        document.add(searchParagraph);
    }

    private void addSummary(Document document, List<InventoryProductEntity> products) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);

        Paragraph section = new Paragraph("Resumen General", sectionFont);
        section.setSpacingBefore(6f);
        section.setSpacingAfter(8f);
        document.add(section);

        long lowStockCount = products.stream()
                .filter(product -> getStatus(product) == InventoryStockStatus.LOW_STOCK)
                .count();

        long outOfStockCount = products.stream()
                .filter(product -> getStatus(product) == InventoryStockStatus.OUT_OF_STOCK)
                .count();

        int totalUnits = products.stream()
                .mapToInt(InventoryProductEntity::getStock)
                .sum();

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(45);
        table.setSpacingAfter(12f);

        table.addCell(buildCell("Productos totales", true));
        table.addCell(buildCell(String.valueOf(products.size()), false));

        table.addCell(buildCell("Unidades totales", true));
        table.addCell(buildCell(String.valueOf(totalUnits), false));

        table.addCell(buildCell("Bajo stock", true));
        table.addCell(buildCell(String.valueOf(lowStockCount), false));

        table.addCell(buildCell("Agotados", true));
        table.addCell(buildCell(String.valueOf(outOfStockCount), false));

        document.add(table);
    }

    private void addDetailTable(Document document, List<InventoryProductEntity> products) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);

        Paragraph section = new Paragraph("Detalle", sectionFont);
        section.setSpacingBefore(6f);
        section.setSpacingAfter(8f);
        document.add(section);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4.5f, 3.0f, 2.3f, 1.8f, 2.2f});

        table.addCell(buildCell("Producto", true));
        table.addCell(buildCell("Categoría", true));
        table.addCell(buildCell("SKU", true));
        table.addCell(buildCell("Stock", true));
        table.addCell(buildCell("Estado", true));

        if (products.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase("No hay productos para los filtros seleccionados."));
            emptyCell.setColspan(5);
            emptyCell.setPadding(8f);
            table.addCell(emptyCell);
        } else {
            for (InventoryProductEntity product : products) {
                table.addCell(buildCell(product.getName(), false));
                table.addCell(buildCell(product.getCategory().getName(), false));
                table.addCell(buildCell(product.getSku() != null ? product.getSku() : "-", false));
                table.addCell(buildCell(String.valueOf(product.getStock()), false));
                table.addCell(buildCell(getStatusLabel(getStatus(product)), false));
            }
        }

        document.add(table);
    }

    private PdfPCell buildCell(String text, boolean header) {
        Font font = header
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)
                : FontFactory.getFont(FontFactory.HELVETICA, 10);

        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6f);
        return cell;
    }

    private InventoryStockStatus getStatus(InventoryProductEntity product) {
        if (product.getStock() == 0) {
            return InventoryStockStatus.OUT_OF_STOCK;
        }
        if (product.getStock() <= product.getMinimumStock()) {
            return InventoryStockStatus.LOW_STOCK;
        }
        return InventoryStockStatus.IN_STOCK;
    }

    private String getStatusLabel(InventoryStockStatus status) {
        return switch (status) {
            case IN_STOCK -> "En Stock";
            case LOW_STOCK -> "Bajo Stock";
            case OUT_OF_STOCK -> "Agotado";
        };
    }
}