package es.pedro.pymes_backend.application;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import es.pedro.pymes_backend.domain.PaymentMethod;
import es.pedro.pymes_backend.domain.SaleEntity;
import es.pedro.pymes_backend.domain.SaleItemType;
import es.pedro.pymes_backend.infrastructure.repository.SaleRepository;
import es.pedro.pymes_backend.infrastructure.web.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SalePdfServiceImpl implements SalePdfService {

    private final SaleRepository saleRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("0.00");

    @Override
    public byte[] generateSalesPdf(String search, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate no puede ser posterior a toDate");
        }

        Specification<SaleEntity> spec = Specification
                .where(SaleSpecification.hasSearch(search))
                .and(SaleSpecification.saleDateFrom(fromDate))
                .and(SaleSpecification.saleDateTo(toDate));

        List<SaleEntity> sales = saleRepository.findAll(
                spec,
                Sort.by(
                        Sort.Order.desc("saleDate"),
                        Sort.Order.desc("id")
                )
        );

        return buildPdf(sales, fromDate, toDate);
    }

    private byte[] buildPdf(List<SaleEntity> sales, LocalDate fromDate, LocalDate toDate) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            addHeader(document, fromDate, toDate);
            addSummary(document, sales);
            addPaymentMethodBreakdown(document, sales);
            addItemTypeBreakdown(document, sales);
            addDetailTable(document, sales);

            document.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de ventas", e);
        }
    }

    private void addHeader(Document document, LocalDate fromDate, LocalDate toDate) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, Color.BLACK);
        Font subtitleFont = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.DARK_GRAY);
        Font infoFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);

        Paragraph title = new Paragraph("Historial de Ventas", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Centro de Estética", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(15f);
        document.add(subtitle);

        String periodText = buildPeriodText(fromDate, toDate);
        Paragraph period = new Paragraph("Periodo: " + periodText, infoFont);
        period.setSpacingAfter(5f);
        document.add(period);

        Paragraph generated = new Paragraph("Generado: " + LocalDate.now().format(DATE_FORMAT), infoFont);
        generated.setSpacingAfter(15f);
        document.add(generated);
    }

    private void addSummary(Document document, List<SaleEntity> sales) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 11, Font.NORMAL);

        Paragraph section = new Paragraph("Resumen General", sectionFont);
        section.setSpacingBefore(5f);
        section.setSpacingAfter(10f);
        document.add(section);

        int totalTransactions = sales.size();
        BigDecimal totalIncome = sales.stream()
                .map(SaleEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = totalTransactions == 0
                ? BigDecimal.ZERO
                : totalIncome.divide(BigDecimal.valueOf(totalTransactions), 2, java.math.RoundingMode.HALF_UP);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15f);
        table.setWidths(new float[]{1f, 1f, 1f});

        table.addCell(buildInfoCell("Total Transacciones", String.valueOf(totalTransactions)));
        table.addCell(buildInfoCell("Ingresos Totales", formatMoney(totalIncome)));
        table.addCell(buildInfoCell("Promedio por Venta", formatMoney(average)));

        document.add(table);
    }

    private void addPaymentMethodBreakdown(Document document, List<SaleEntity> sales) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);

        Paragraph section = new Paragraph("Desglose por Método de Pago", sectionFont);
        section.setSpacingBefore(5f);
        section.setSpacingAfter(10f);
        document.add(section);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15f);
        table.setWidths(new float[]{3f, 1f});

        table.addCell(buildHeaderCell("Método"));
        table.addCell(buildHeaderCell("Monto"));

        Map<PaymentMethod, BigDecimal> totals = new EnumMap<>(PaymentMethod.class);
        BigDecimal nullTotal = BigDecimal.ZERO;

        for (SaleEntity sale : sales) {
            if (sale.getPaymentMethod() == null) {
                nullTotal = nullTotal.add(sale.getTotalAmount());
            } else {
                totals.merge(sale.getPaymentMethod(), sale.getTotalAmount(), BigDecimal::add);
            }
        }

        for (PaymentMethod method : PaymentMethod.values()) {
            table.addCell(buildBodyCell(paymentMethodToText(method)));
            table.addCell(buildBodyCell(formatMoney(totals.getOrDefault(method, BigDecimal.ZERO))));
        }

        table.addCell(buildBodyCell("Sin método"));
        table.addCell(buildBodyCell(formatMoney(nullTotal)));

        document.add(table);
    }

    private void addItemTypeBreakdown(Document document, List<SaleEntity> sales) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);

        Paragraph section = new Paragraph("Desglose por Tipo", sectionFont);
        section.setSpacingBefore(5f);
        section.setSpacingAfter(10f);
        document.add(section);

        BigDecimal serviceTotal = sales.stream()
                .filter(sale -> sale.getItemType() == SaleItemType.SERVICE)
                .map(SaleEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal productTotal = sales.stream()
                .filter(sale -> sale.getItemType() == SaleItemType.PRODUCT)
                .map(SaleEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15f);
        table.setWidths(new float[]{3f, 1f});

        table.addCell(buildHeaderCell("Tipo"));
        table.addCell(buildHeaderCell("Monto"));

        table.addCell(buildBodyCell("Servicios"));
        table.addCell(buildBodyCell(formatMoney(serviceTotal)));

        table.addCell(buildBodyCell("Productos"));
        table.addCell(buildBodyCell(formatMoney(productTotal)));

        document.add(table);
    }

    private void addDetailTable(Document document, List<SaleEntity> sales) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);

        Paragraph section = new Paragraph("Transacciones Detalladas", sectionFont);
        section.setSpacingBefore(5f);
        section.setSpacingAfter(10f);
        document.add(section);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.3f, 1.6f, 2.2f, 1.4f, 0.8f, 1f, 1f});

        table.addCell(buildHeaderCell("Fecha"));
        table.addCell(buildHeaderCell("Cliente"));
        table.addCell(buildHeaderCell("Servicio/Producto"));
        table.addCell(buildHeaderCell("Método"));
        table.addCell(buildHeaderCell("Cant."));
        table.addCell(buildHeaderCell("P. Unit."));
        table.addCell(buildHeaderCell("Total"));

        if (sales.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase("No hay ventas para el filtro seleccionado"));
            emptyCell.setColspan(7);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            emptyCell.setPadding(10f);
            table.addCell(emptyCell);
        } else {
            for (SaleEntity sale : sales) {
                table.addCell(buildBodyCell(sale.getSaleDate().format(DATE_FORMAT)));
                table.addCell(buildBodyCell(sale.getClientName()));
                table.addCell(buildBodyCell(sale.getItemName()));
                table.addCell(buildBodyCell(paymentMethodToText(sale.getPaymentMethod())));
                table.addCell(buildBodyCell(String.valueOf(sale.getQuantity())));
                table.addCell(buildBodyCell(formatMoney(sale.getUnitPrice())));
                table.addCell(buildBodyCell(formatMoney(sale.getTotalAmount())));
            }
        }

        document.add(table);
    }

    private PdfPCell buildHeaderCell(String text) {
        Font font = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(37, 99, 235));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8f);
        return cell;
    }

    private PdfPCell buildBodyCell(String text) {
        Font font = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(6f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell buildInfoCell(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10f);

        Font labelFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
        Font valueFont = new Font(Font.HELVETICA, 14, Font.BOLD, Color.BLACK);

        Paragraph labelParagraph = new Paragraph(label, labelFont);
        Paragraph valueParagraph = new Paragraph(value, valueFont);

        cell.addElement(labelParagraph);
        cell.addElement(valueParagraph);

        return cell;
    }

    private String formatMoney(BigDecimal amount) {
        return "$" + MONEY_FORMAT.format(amount);
    }

    private String buildPeriodText(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null) {
            return fromDate.format(DATE_FORMAT) + " - " + toDate.format(DATE_FORMAT);
        }
        if (fromDate != null) {
            return "Desde " + fromDate.format(DATE_FORMAT);
        }
        if (toDate != null) {
            return "Hasta " + toDate.format(DATE_FORMAT);
        }
        return "Todo el histórico";
    }

    private String paymentMethodToText(PaymentMethod method) {
        if (method == null) {
            return "Sin método";
        }

        return switch (method) {
            case CASH -> "Efectivo";
            case DEBIT_CARD -> "Tarjeta débito";
            case CREDIT_CARD -> "Tarjeta crédito";
            case BANK_TRANSFER -> "Transferencia";
            case BIZUM -> "Bizum";
        };
    }
}
