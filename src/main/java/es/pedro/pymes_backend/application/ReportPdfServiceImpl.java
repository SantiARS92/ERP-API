package es.pedro.pymes_backend.application;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import es.pedro.pymes_backend.domain.BusinessSettingsEntity;
import es.pedro.pymes_backend.infrastructure.repository.BusinessSettingsRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.AnnualConsolidatedReportResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.AnnualMonthlyBreakdownResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.MonthlyDetailedReportResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.QuarterlyDetailedReportResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.QuarterlyMonthlyBreakdownResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Month;
import java.util.Currency;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportPdfServiceImpl implements ReportPdfService {

    private static final Locale LOCALE_ES = new Locale("es", "ES");

    private static final Color PRIMARY = new Color(11, 31, 64);
    private static final Color SUCCESS = new Color(22, 163, 74);
    private static final Color DANGER = new Color(220, 38, 38);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color LIGHT_BG = new Color(248, 250, 252);
    private static final Color DARK_BOX = new Color(15, 23, 42);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PRIMARY);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11, MUTED);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, PRIMARY);
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11, MUTED);
    private static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PRIMARY);
    private static final Font VALUE_SUCCESS_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, SUCCESS);
    private static final Font VALUE_DANGER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, DANGER);
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY);
    private static final Font NOTE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
    private static final Font TOTAL_BOX_LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
    private static final Font TOTAL_BOX_VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.WHITE);

    private final ReportsService reportsService;
    private final BusinessSettingsRepository businessSettingsRepository;

    @Override
    public byte[] generateMonthlyDetailedPdf(int year, int month) {
        MonthlyDetailedReportResponse report = reportsService.getMonthlyDetailedReport(year, month);
        BusinessSettingsEntity settings = businessSettingsRepository.findFirstByOrderByIdAsc().orElse(null);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 45, 45);

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            addGenericHeader(
                    document,
                    settings,
                    "INFORME MENSUAL DETALLADO",
                    formatMonthYear(year, month),
                    "INF-M-" + year + "-" + String.format("%02d", month)
            );

            addMonthlySummarySection(document, report);
            addMonthlyTaxBreakdownSection(document, report);
            addFinalTaxesBox(document, report.getEstimatedTotalTaxes());
            addNoteSection(document);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar el PDF del informe mensual detallado", e);
        }

        return outputStream.toByteArray();
    }

    @Override
    public byte[] generateQuarterlyDetailedPdf(int year, int quarter) {
        QuarterlyDetailedReportResponse report = reportsService.getQuarterlyDetailedReport(year, quarter);
        BusinessSettingsEntity settings = businessSettingsRepository.findFirstByOrderByIdAsc().orElse(null);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 45, 45);

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            addGenericHeader(
                    document,
                    settings,
                    "INFORME TRIMESTRAL DETALLADO",
                    report.getQuarterLabel(),
                    "INF-T-" + year + "-Q" + quarter
            );

            addQuarterlySummarySection(document, report);
            addQuarterlyMonthlyBreakdownSection(document, report);
            addQuarterlyTaxBreakdownSection(document, report);
            addFinalTaxesBox(document, report.getEstimatedTotalTaxes());
            addNoteSection(document);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar el PDF del informe trimestral detallado", e);
        }

        return outputStream.toByteArray();
    }

    @Override
    public byte[] generateAnnualConsolidatedPdf(int year) {
        AnnualConsolidatedReportResponse report = reportsService.getAnnualConsolidatedReport(year);
        BusinessSettingsEntity settings = businessSettingsRepository.findFirstByOrderByIdAsc().orElse(null);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 32, 32, 40, 40);

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            addGenericHeader(
                    document,
                    settings,
                    "INFORME ANUAL CONSOLIDADO",
                    "Año " + year,
                    "INF-A-" + year
            );

            addAnnualSummarySection(document, report);
            addAnnualMonthlyBreakdownSection(document, report);
            addAnnualTaxBreakdownSection(document, report);
            addFinalTaxesBox(document, report.getEstimatedTotalTaxes());
            addNoteSection(document);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar el PDF del informe anual consolidado", e);
        }

        return outputStream.toByteArray();
    }

    private void addGenericHeader(
            Document document,
            BusinessSettingsEntity settings,
            String reportTitle,
            String periodLabel,
            String reference
    ) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{58f, 42f});
        headerTable.setSpacingAfter(16f);

        String businessName = settings != null && hasText(settings.getBusinessName())
                ? settings.getBusinessName()
                : "Centro de Estética";

        String address = settings != null && hasText(settings.getAddress())
                ? settings.getAddress()
                : "Dirección no configurada";

        String phone = settings != null && hasText(settings.getPhone())
                ? settings.getPhone()
                : "-";

        String email = settings != null && hasText(settings.getContactEmail())
                ? settings.getContactEmail()
                : "-";

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0);

        Paragraph businessParagraph = new Paragraph();
        businessParagraph.add(new Phrase(businessName + "\n", TITLE_FONT));
        businessParagraph.add(new Phrase(address + "\n", SUBTITLE_FONT));
        businessParagraph.add(new Phrase("Tel: " + phone + "\n", SUBTITLE_FONT));
        businessParagraph.add(new Phrase("Email: " + email, SUBTITLE_FONT));
        leftCell.addElement(businessParagraph);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(0);

        Paragraph reportParagraph = new Paragraph();
        reportParagraph.setAlignment(Element.ALIGN_RIGHT);
        reportParagraph.add(new Phrase(reportTitle + "\n", TITLE_FONT));
        reportParagraph.add(new Phrase("Período: " + periodLabel + "\n", SUBTITLE_FONT));
        reportParagraph.add(new Phrase("Ref: " + reference, SUBTITLE_FONT));
        rightCell.addElement(reportParagraph);

        headerTable.addCell(leftCell);
        headerTable.addCell(rightCell);

        document.add(headerTable);

        LineSeparator separator = new LineSeparator();
        separator.setLineColor(BORDER);
        separator.setLineWidth(1f);
        document.add(new Chunk(separator));
        document.add(spaceParagraph(12f));
    }

    private void addMonthlySummarySection(Document document, MonthlyDetailedReportResponse report) throws DocumentException {
        Paragraph title = new Paragraph("Resumen financiero", SECTION_FONT);
        title.setSpacingAfter(12f);
        document.add(title);

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new float[]{50f, 50f});
        summaryTable.setSpacingAfter(18f);

        summaryTable.addCell(createMetricCell("Ingresos totales", formatCurrency(report.getGrossIncome()), VALUE_FONT));
        summaryTable.addCell(createMetricCell("Gastos totales", formatCurrency(report.getGrossExpenses()), VALUE_FONT));
        summaryTable.addCell(createMetricCell("Beneficio operativo", formatCurrency(report.getOperatingProfit()), pickAmountFont(report.getOperatingProfit())));
        summaryTable.addCell(createMetricCell("Ratio de rentabilidad", formatPercent(report.getProfitabilityRatio()), pickAmountFont(report.getProfitabilityRatio())));

        document.add(summaryTable);
    }

    private void addMonthlyTaxBreakdownSection(Document document, MonthlyDetailedReportResponse report) throws DocumentException {
        Paragraph title = new Paragraph("Desglose fiscal estimado", SECTION_FONT);
        title.setSpacingAfter(12f);
        document.add(title);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{68f, 32f});
        table.setSpacingAfter(18f);

        table.addCell(createHeaderCell("Concepto"));
        table.addCell(createHeaderCell("Importe", Element.ALIGN_RIGHT));

        BigDecimal estimatedTaxableProfit = scale(report.getEstimatedIncomeBase().subtract(report.getEstimatedExpenseBase()));

        table.addCell(createBodyCell("Base imponible estimada de ingresos"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedIncomeBase()), Element.ALIGN_RIGHT));

        table.addCell(createBodyCell("Base imponible estimada de gastos"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedExpenseBase()), Element.ALIGN_RIGHT));

        table.addCell(createBodyCell("IVA repercutido estimado"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedOutputVat()), Element.ALIGN_RIGHT));

        table.addCell(createBodyCell("IVA soportado estimado"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedInputVat()), Element.ALIGN_RIGHT));

        table.addCell(createBodyCell("IVA estimado a pagar"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedVatPayable()), Element.ALIGN_RIGHT, pickAmountColor(report.getEstimatedVatPayable())));

        table.addCell(createBodyCell("Beneficio estimado antes de IRPF"));
        table.addCell(createBodyCell(formatCurrency(estimatedTaxableProfit), Element.ALIGN_RIGHT, pickAmountColor(estimatedTaxableProfit)));

        table.addCell(createBodyCell("IRPF estimado (15%)"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedIrpfPayable()), Element.ALIGN_RIGHT, pickAmountColor(report.getEstimatedIrpfPayable())));

        document.add(table);
    }

    private void addQuarterlySummarySection(Document document, QuarterlyDetailedReportResponse report) throws DocumentException {
        Paragraph title = new Paragraph("Resumen financiero del trimestre", SECTION_FONT);
        title.setSpacingAfter(12f);
        document.add(title);

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new float[]{50f, 50f});
        summaryTable.setSpacingAfter(18f);

        summaryTable.addCell(createMetricCell("Ingresos del trimestre", formatCurrency(report.getGrossIncome()), VALUE_FONT));
        summaryTable.addCell(createMetricCell("Gastos del trimestre", formatCurrency(report.getGrossExpenses()), VALUE_FONT));
        summaryTable.addCell(createMetricCell("Beneficio operativo", formatCurrency(report.getOperatingProfit()), pickAmountFont(report.getOperatingProfit())));
        summaryTable.addCell(createMetricCell("Ratio de rentabilidad", formatPercent(report.getProfitabilityRatio()), pickAmountFont(report.getProfitabilityRatio())));

        document.add(summaryTable);
    }

    private void addQuarterlyMonthlyBreakdownSection(Document document, QuarterlyDetailedReportResponse report) throws DocumentException {
        Paragraph title = new Paragraph("Desglose mensual del trimestre", SECTION_FONT);
        title.setSpacingAfter(12f);
        document.add(title);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{22f, 20f, 20f, 20f, 18f});
        table.setSpacingAfter(18f);

        table.addCell(createHeaderCell("Mes"));
        table.addCell(createHeaderCell("Ingresos", Element.ALIGN_RIGHT));
        table.addCell(createHeaderCell("Gastos", Element.ALIGN_RIGHT));
        table.addCell(createHeaderCell("Beneficio", Element.ALIGN_RIGHT));
        table.addCell(createHeaderCell("IVA est.", Element.ALIGN_RIGHT));

        for (QuarterlyMonthlyBreakdownResponse row : report.getMonthlyBreakdown()) {
            table.addCell(createBodyCell(row.getMonthLabel()));
            table.addCell(createBodyCell(formatCurrency(row.getGrossIncome()), Element.ALIGN_RIGHT));
            table.addCell(createBodyCell(formatCurrency(row.getGrossExpenses()), Element.ALIGN_RIGHT));
            table.addCell(createBodyCell(formatCurrency(row.getOperatingProfit()), Element.ALIGN_RIGHT, pickAmountColor(row.getOperatingProfit())));
            table.addCell(createBodyCell(formatCurrency(row.getEstimatedVatPayable()), Element.ALIGN_RIGHT, pickAmountColor(row.getEstimatedVatPayable())));
        }

        table.addCell(createFooterCell("Total trimestre"));
        table.addCell(createFooterCell(formatCurrency(report.getGrossIncome()), Element.ALIGN_RIGHT));
        table.addCell(createFooterCell(formatCurrency(report.getGrossExpenses()), Element.ALIGN_RIGHT));
        table.addCell(createFooterCell(formatCurrency(report.getOperatingProfit()), Element.ALIGN_RIGHT));
        table.addCell(createFooterCell(formatCurrency(report.getEstimatedVatPayable()), Element.ALIGN_RIGHT));

        document.add(table);
    }

    private void addQuarterlyTaxBreakdownSection(Document document, QuarterlyDetailedReportResponse report) throws DocumentException {
        Paragraph title = new Paragraph("Resumen fiscal estimado", SECTION_FONT);
        title.setSpacingAfter(12f);
        document.add(title);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{68f, 32f});
        table.setSpacingAfter(18f);

        table.addCell(createHeaderCell("Concepto"));
        table.addCell(createHeaderCell("Importe", Element.ALIGN_RIGHT));

        table.addCell(createBodyCell("Base imponible estimada de ingresos"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedIncomeBase()), Element.ALIGN_RIGHT));

        table.addCell(createBodyCell("Base imponible estimada de gastos"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedExpenseBase()), Element.ALIGN_RIGHT));

        table.addCell(createBodyCell("IVA repercutido estimado"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedOutputVat()), Element.ALIGN_RIGHT));

        table.addCell(createBodyCell("IVA soportado estimado"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedInputVat()), Element.ALIGN_RIGHT));

        table.addCell(createBodyCell("IVA estimado a pagar"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedVatPayable()), Element.ALIGN_RIGHT, pickAmountColor(report.getEstimatedVatPayable())));

        table.addCell(createBodyCell("IRPF estimado (15%)"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedIrpfPayable()), Element.ALIGN_RIGHT, pickAmountColor(report.getEstimatedIrpfPayable())));

        document.add(table);
    }

    private void addAnnualSummarySection(Document document, AnnualConsolidatedReportResponse report) throws DocumentException {
        Paragraph title = new Paragraph("Resumen financiero anual", SECTION_FONT);
        title.setSpacingAfter(12f);
        document.add(title);

        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new float[]{25f, 25f, 25f, 25f});
        summaryTable.setSpacingAfter(18f);

        summaryTable.addCell(createMetricCell("Ingresos anuales", formatCurrency(report.getGrossIncome()), VALUE_FONT));
        summaryTable.addCell(createMetricCell("Gastos anuales", formatCurrency(report.getGrossExpenses()), VALUE_FONT));
        summaryTable.addCell(createMetricCell("Beneficio operativo", formatCurrency(report.getOperatingProfit()), pickAmountFont(report.getOperatingProfit())));
        summaryTable.addCell(createMetricCell("Rentabilidad", formatPercent(report.getProfitabilityRatio()), pickAmountFont(report.getProfitabilityRatio())));

        document.add(summaryTable);
    }

    private void addAnnualMonthlyBreakdownSection(Document document, AnnualConsolidatedReportResponse report) throws DocumentException {
        Paragraph title = new Paragraph("Desglose mensual del año", SECTION_FONT);
        title.setSpacingAfter(12f);
        document.add(title);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{18f, 17f, 17f, 17f, 15f, 16f});
        table.setSpacingAfter(18f);

        table.addCell(createHeaderCell("Mes"));
        table.addCell(createHeaderCell("Ingresos", Element.ALIGN_RIGHT));
        table.addCell(createHeaderCell("Gastos", Element.ALIGN_RIGHT));
        table.addCell(createHeaderCell("Beneficio", Element.ALIGN_RIGHT));
        table.addCell(createHeaderCell("IVA est.", Element.ALIGN_RIGHT));
        table.addCell(createHeaderCell("IRPF est.", Element.ALIGN_RIGHT));

        for (AnnualMonthlyBreakdownResponse row : report.getMonthlyBreakdown()) {
            table.addCell(createBodyCell(row.getMonthLabel()));
            table.addCell(createBodyCell(formatCurrency(row.getGrossIncome()), Element.ALIGN_RIGHT));
            table.addCell(createBodyCell(formatCurrency(row.getGrossExpenses()), Element.ALIGN_RIGHT));
            table.addCell(createBodyCell(formatCurrency(row.getOperatingProfit()), Element.ALIGN_RIGHT, pickAmountColor(row.getOperatingProfit())));
            table.addCell(createBodyCell(formatCurrency(row.getEstimatedVatPayable()), Element.ALIGN_RIGHT, pickAmountColor(row.getEstimatedVatPayable())));
            table.addCell(createBodyCell(formatCurrency(row.getEstimatedIrpfPayable()), Element.ALIGN_RIGHT, pickAmountColor(row.getEstimatedIrpfPayable())));
        }

        table.addCell(createFooterCell("Total anual"));
        table.addCell(createFooterCell(formatCurrency(report.getGrossIncome()), Element.ALIGN_RIGHT));
        table.addCell(createFooterCell(formatCurrency(report.getGrossExpenses()), Element.ALIGN_RIGHT));
        table.addCell(createFooterCell(formatCurrency(report.getOperatingProfit()), Element.ALIGN_RIGHT));
        table.addCell(createFooterCell(formatCurrency(report.getEstimatedVatPayable()), Element.ALIGN_RIGHT));
        table.addCell(createFooterCell(formatCurrency(report.getEstimatedIrpfPayable()), Element.ALIGN_RIGHT));

        document.add(table);
    }

    private void addAnnualTaxBreakdownSection(Document document, AnnualConsolidatedReportResponse report) throws DocumentException {
        Paragraph title = new Paragraph("Resumen fiscal estimado anual", SECTION_FONT);
        title.setSpacingAfter(12f);
        document.add(title);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{68f, 32f});
        table.setSpacingAfter(18f);

        table.addCell(createHeaderCell("Concepto"));
        table.addCell(createHeaderCell("Importe", Element.ALIGN_RIGHT));

        table.addCell(createBodyCell("Base imponible estimada de ingresos"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedIncomeBase()), Element.ALIGN_RIGHT));

        table.addCell(createBodyCell("Base imponible estimada de gastos"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedExpenseBase()), Element.ALIGN_RIGHT));

        table.addCell(createBodyCell("IVA repercutido estimado"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedOutputVat()), Element.ALIGN_RIGHT));

        table.addCell(createBodyCell("IVA soportado estimado"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedInputVat()), Element.ALIGN_RIGHT));

        table.addCell(createBodyCell("IVA estimado a pagar"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedVatPayable()), Element.ALIGN_RIGHT, pickAmountColor(report.getEstimatedVatPayable())));

        table.addCell(createBodyCell("IRPF estimado anual (15%)"));
        table.addCell(createBodyCell(formatCurrency(report.getEstimatedIrpfPayable()), Element.ALIGN_RIGHT, pickAmountColor(report.getEstimatedIrpfPayable())));

        document.add(table);
    }

    private void addFinalTaxesBox(Document document, BigDecimal totalTaxes) throws DocumentException {
        PdfPTable totalBoxTable = new PdfPTable(1);
        totalBoxTable.setWidthPercentage(100);
        totalBoxTable.setSpacingAfter(18f);

        PdfPCell totalCell = new PdfPCell();
        totalCell.setBorder(Rectangle.NO_BORDER);
        totalCell.setBackgroundColor(DARK_BOX);
        totalCell.setPaddingTop(16f);
        totalCell.setPaddingBottom(18f);
        totalCell.setPaddingLeft(18f);
        totalCell.setPaddingRight(18f);

        Paragraph label = new Paragraph("TOTAL ESTIMADO A PAGAR", TOTAL_BOX_LABEL_FONT);
        label.setSpacingAfter(8f);

        Paragraph value = new Paragraph(formatCurrency(totalTaxes), TOTAL_BOX_VALUE_FONT);

        totalCell.addElement(label);
        totalCell.addElement(value);

        totalBoxTable.addCell(totalCell);
        document.add(totalBoxTable);
    }

    private void addNoteSection(Document document) throws DocumentException {
        PdfPTable noteTable = new PdfPTable(1);
        noteTable.setWidthPercentage(100);

        PdfPCell noteCell = new PdfPCell();
        noteCell.setBackgroundColor(LIGHT_BG);
        noteCell.setBorderColor(BORDER);
        noteCell.setPadding(12f);

        Paragraph note = new Paragraph(
                "Nota: Este informe usa cálculos fiscales estimados con IVA del 21% e IRPF del 15%. " +
                        "Debe revisarse con un gestor antes de usarlo como documento fiscal oficial.",
                NOTE_FONT
        );
        note.setLeading(14f);

        noteCell.addElement(note);
        noteTable.addCell(noteCell);

        document.add(noteTable);
    }

    private PdfPCell createMetricCell(String label, String value, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(Color.WHITE);
        cell.setBorderColor(BORDER);
        cell.setPaddingTop(14f);
        cell.setPaddingBottom(14f);
        cell.setPaddingLeft(14f);
        cell.setPaddingRight(14f);
        cell.setMinimumHeight(78f);

        Paragraph labelParagraph = new Paragraph(label, LABEL_FONT);
        labelParagraph.setSpacingAfter(8f);

        Paragraph valueParagraph = new Paragraph(value, valueFont);

        cell.addElement(labelParagraph);
        cell.addElement(valueParagraph);
        return cell;
    }

    private PdfPCell createHeaderCell(String text) {
        return createHeaderCell(text, Element.ALIGN_LEFT);
    }

    private PdfPCell createHeaderCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(LIGHT_BG);
        cell.setBorderColor(BORDER);
        cell.setPaddingTop(10f);
        cell.setPaddingBottom(10f);
        cell.setPaddingLeft(10f);
        cell.setPaddingRight(10f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell createBodyCell(String text) {
        return createBodyCell(text, Element.ALIGN_LEFT, PRIMARY);
    }

    private PdfPCell createBodyCell(String text, int alignment) {
        return createBodyCell(text, alignment, PRIMARY);
    }

    private PdfPCell createBodyCell(String text, int alignment, Color color) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 10, color);

        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderColor(BORDER);
        cell.setPaddingTop(10f);
        cell.setPaddingBottom(10f);
        cell.setPaddingLeft(10f);
        cell.setPaddingRight(10f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell createFooterCell(String text) {
        return createFooterCell(text, Element.ALIGN_LEFT);
    }

    private PdfPCell createFooterCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(LIGHT_BG);
        cell.setBorderColor(BORDER);
        cell.setPaddingTop(11f);
        cell.setPaddingBottom(11f);
        cell.setPaddingLeft(10f);
        cell.setPaddingRight(10f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private Font pickAmountFont(BigDecimal amount) {
        if (amount == null) {
            return VALUE_FONT;
        }
        return amount.compareTo(BigDecimal.ZERO) < 0 ? VALUE_DANGER_FONT : VALUE_SUCCESS_FONT;
    }

    private Color pickAmountColor(BigDecimal amount) {
        if (amount == null) {
            return PRIMARY;
        }
        return amount.compareTo(BigDecimal.ZERO) < 0 ? DANGER : SUCCESS;
    }

    private Paragraph spaceParagraph(float spacingAfter) {
        Paragraph paragraph = new Paragraph(" ");
        paragraph.setSpacingAfter(spacingAfter);
        return paragraph;
    }

    private String formatMonthYear(int year, int month) {
        String monthName = Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, LOCALE_ES);
        monthName = monthName.substring(0, 1).toUpperCase(LOCALE_ES) + monthName.substring(1);
        return monthName + " " + year;
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(LOCALE_ES);
        currencyFormat.setCurrency(Currency.getInstance("EUR"));
        return currencyFormat.format(amount == null ? BigDecimal.ZERO : amount);
    }

    private String formatPercent(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return safeAmount.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private BigDecimal scale(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}