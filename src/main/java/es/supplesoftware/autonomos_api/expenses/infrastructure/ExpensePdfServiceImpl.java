package es.supplesoftware.autonomos_api.expenses.infrastructure;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import es.supplesoftware.autonomos_api.expenses.domain.enums.ExpenseCategory;

import es.supplesoftware.autonomos_api.expenses.infrastructure.ExpenseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpensePdfServiceImpl implements ExpensePdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public byte[] generateExpensePdf(List<ExpenseEntity> expenses, LocalDate fromDate, LocalDate toDate, String search) {
        try {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            PdfWriter.getInstance(document, outputStream);
            document.open();

            addHeader(document, fromDate, toDate, search);
            addGeneralSummary(document, expenses);
            addCategorySummary(document, expenses);
            addDetailTable(document, expenses);

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de gastos", e);
        }
    }

    private void addHeader(Document document, LocalDate fromDate, LocalDate toDate, String search) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

        Paragraph title = new Paragraph("Historial de Gastos", titleFont);
        title.setSpacingAfter(10f);
        document.add(title);

        String periodText = buildPeriodText(fromDate, toDate);
        Paragraph period = new Paragraph("Periodo: " + periodText, textFont);
        period.setSpacingAfter(4f);
        document.add(period);

        String searchText = StringUtils.hasText(search) ? search.trim() : "Sin filtro";
        Paragraph searchParagraph = new Paragraph("Búsqueda: " + searchText, textFont);
        searchParagraph.setSpacingAfter(12f);
        document.add(searchParagraph);
    }

    private void addGeneralSummary(Document document, List<ExpenseEntity> expenses) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

        Paragraph section = new Paragraph("Resumen General", sectionFont);
        section.setSpacingBefore(6f);
        section.setSpacingAfter(8f);
        document.add(section);

        BigDecimal totalAmount = expenses.stream()
                .map(ExpenseEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(45);
        table.setSpacingAfter(12f);

        table.addCell(buildCell("Total de gastos", true));
        table.addCell(buildCell(String.valueOf(expenses.size()), false));

        table.addCell(buildCell("Importe acumulado", true));
        table.addCell(buildCell(formatMoney(totalAmount), false));

        document.add(table);
    }

    private void addCategorySummary(Document document, List<ExpenseEntity> expenses) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);

        Paragraph section = new Paragraph("Resumen por Categoría", sectionFont);
        section.setSpacingBefore(6f);
        section.setSpacingAfter(8f);
        document.add(section);

        Map<ExpenseCategory, BigDecimal> totalsByCategory = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory category : ExpenseCategory.values()) {
            totalsByCategory.put(category, BigDecimal.ZERO);
        }

        for (ExpenseEntity expense : expenses) {
            totalsByCategory.put(
                    expense.getCategory(),
                    totalsByCategory.get(expense.getCategory()).add(expense.getAmount())
            );
        }

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(45);
        table.setSpacingAfter(12f);

        for (ExpenseCategory category : ExpenseCategory.values()) {
            table.addCell(buildCell(getCategoryLabel(category), true));
            table.addCell(buildCell(formatMoney(totalsByCategory.get(category)), false));
        }

        document.add(table);
    }

    private void addDetailTable(Document document, List<ExpenseEntity> expenses) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);

        Paragraph section = new Paragraph("Detalle", sectionFont);
        section.setSpacingBefore(6f);
        section.setSpacingAfter(8f);
        document.add(section);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.2f, 6.0f, 2.5f, 2.3f});

        table.addCell(buildCell("Fecha", true));
        table.addCell(buildCell("Concepto", true));
        table.addCell(buildCell("Categoría", true));
        table.addCell(buildCell("Importe", true));

        if (expenses.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase("No hay gastos para los filtros seleccionados."));
            emptyCell.setColspan(4);
            emptyCell.setPadding(8f);
            table.addCell(emptyCell);
        } else {
            for (ExpenseEntity expense : expenses) {
                table.addCell(buildCell(expense.getExpenseDate().format(DATE_FORMATTER), false));
                table.addCell(buildCell(expense.getConcept(), false));
                table.addCell(buildCell(getCategoryLabel(expense.getCategory()), false));
                table.addCell(buildCell(formatMoney(expense.getAmount()), false));
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

    private String buildPeriodText(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            return "Sin rango";
        }

        if (fromDate != null && toDate != null) {
            return fromDate.format(DATE_FORMATTER) + " - " + toDate.format(DATE_FORMATTER);
        }

        if (fromDate != null) {
            return "Desde " + fromDate.format(DATE_FORMATTER);
        }

        return "Hasta " + toDate.format(DATE_FORMATTER);
    }

    private String formatMoney(BigDecimal amount) {
        return "$" + amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String getCategoryLabel(ExpenseCategory category) {
        return switch (category) {
            case SUPPLIES -> "Suministros";
            case SERVICES -> "Servicios";
            case OTHER -> "Otros";
        };
    }
}