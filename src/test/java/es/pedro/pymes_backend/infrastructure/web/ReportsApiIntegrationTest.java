package es.pedro.pymes_backend.infrastructure.web;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import es.pedro.pymes_backend.domain.AppointmentEntity;
import es.pedro.pymes_backend.domain.AppointmentStatus;
import es.pedro.pymes_backend.domain.CategoryEntity;
import es.pedro.pymes_backend.domain.ClientEntity;
import es.pedro.pymes_backend.domain.ExpenseCategory;
import es.pedro.pymes_backend.domain.ExpenseEntity;
import es.pedro.pymes_backend.domain.InventoryProductEntity;
import es.pedro.pymes_backend.domain.SaleEntity;
import es.pedro.pymes_backend.domain.SaleItemType;
import es.pedro.pymes_backend.domain.SaleSourceType;
import es.pedro.pymes_backend.domain.ServiceEntity;
import es.pedro.pymes_backend.infrastructure.repository.AppointmentRepository;
import es.pedro.pymes_backend.infrastructure.repository.CategoryRepository;
import es.pedro.pymes_backend.infrastructure.repository.ClientRepository;
import es.pedro.pymes_backend.infrastructure.repository.ExpenseRepository;
import es.pedro.pymes_backend.infrastructure.repository.InventoryProductRepository;
import es.pedro.pymes_backend.infrastructure.repository.SaleRepository;
import es.pedro.pymes_backend.infrastructure.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportsApiIntegrationTest extends AuthenticatedApiIntegrationTestSupport {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private InventoryProductRepository inventoryProductRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private SaleRepository saleRepository;

    private MockMvc mockMvc;

    private ClientEntity reportsClient;
    private ServiceEntity facialService;
    private ServiceEntity manicureService;
    private InventoryProductEntity serumProduct;
    private InventoryProductEntity shampooProduct;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = buildAuthenticatedMockMvc();
        seedReportsData();
    }

    @Test
    void monthlySimpleReportShouldReturnMarchSummaryWithCollections() throws Exception {
        mockMvc.perform(get("/api/reports/monthly/simple")
                        .param("year", "2026")
                        .param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(270.0))
                .andExpect(jsonPath("$.totalExpenses").value(90.75))
                .andExpect(jsonPath("$.totalConfirmedAppointments").value(4))
                .andExpect(jsonPath("$.lastThreeMonths", hasSize(3)))
                .andExpect(jsonPath("$.lastThreeMonths[0].period").value("2026-01"))
                .andExpect(jsonPath("$.lastThreeMonths[0].income").value(50.0))
                .andExpect(jsonPath("$.lastThreeMonths[0].expenses").value(20.0))
                .andExpect(jsonPath("$.lastThreeMonths[0].confirmedAppointments").value(1))
                .andExpect(jsonPath("$.lastThreeMonths[1].period").value("2026-02"))
                .andExpect(jsonPath("$.lastThreeMonths[1].income").value(120.0))
                .andExpect(jsonPath("$.lastThreeMonths[1].expenses").value(40.0))
                .andExpect(jsonPath("$.lastThreeMonths[1].confirmedAppointments").value(1))
                .andExpect(jsonPath("$.lastThreeMonths[2].period").value("2026-03"))
                .andExpect(jsonPath("$.lastThreeMonths[2].income").value(270.0))
                .andExpect(jsonPath("$.lastThreeMonths[2].expenses").value(90.75))
                .andExpect(jsonPath("$.lastThreeMonths[2].confirmedAppointments").value(4))
                .andExpect(jsonPath("$.topServices", hasSize(2)))
                .andExpect(jsonPath("$.topServices[0].name").value("Facial Premium"))
                .andExpect(jsonPath("$.topServices[0].totalCount").value(2))
                .andExpect(jsonPath("$.topServices[0].generatedIncome").value(100.0))
                .andExpect(jsonPath("$.topServices[0].averageDurationMinutes").value(60))
                .andExpect(jsonPath("$.topServices[1].name").value("Manicura Express"))
                .andExpect(jsonPath("$.topServices[1].totalCount").value(2))
                .andExpect(jsonPath("$.topServices[1].generatedIncome").value(80.0))
                .andExpect(jsonPath("$.topServices[1].averageDurationMinutes").value(45))
                .andExpect(jsonPath("$.busiestDays", hasSize(3)))
                .andExpect(jsonPath("$.busiestDays[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.busiestDays[0].confirmedAppointments").value(2))
                .andExpect(jsonPath("$.busiestDays[0].percentage").value(50.0))
                .andExpect(jsonPath("$.busiestDays[1].dayOfWeek").value("TUESDAY"))
                .andExpect(jsonPath("$.busiestDays[1].confirmedAppointments").value(1))
                .andExpect(jsonPath("$.busiestDays[1].percentage").value(25.0))
                .andExpect(jsonPath("$.busiestDays[2].dayOfWeek").value("FRIDAY"))
                .andExpect(jsonPath("$.busiestDays[2].confirmedAppointments").value(1))
                .andExpect(jsonPath("$.busiestDays[2].percentage").value(25.0));
    }

    @Test
    void monthlyDetailedReportShouldReturnMarchFinancialBreakdown() throws Exception {
        mockMvc.perform(get("/api/reports/monthly/detailed")
                        .param("year", "2026")
                        .param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossIncome").value(270.0))
                .andExpect(jsonPath("$.grossExpenses").value(90.75))
                .andExpect(jsonPath("$.operatingProfit").value(179.25))
                .andExpect(jsonPath("$.estimatedVatPayable").value(31.11))
                .andExpect(jsonPath("$.estimatedIrpfPayable").value(22.22))
                .andExpect(jsonPath("$.estimatedTotalTaxes").value(53.33))
                .andExpect(jsonPath("$.profitabilityRatio").value(66.39));
    }

    @Test
    void quarterlyDetailedReportShouldReturnFirstQuarterFinancialBreakdown() throws Exception {
        mockMvc.perform(get("/api/reports/quarterly/detailed")
                        .param("year", "2026")
                        .param("quarter", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossIncome").value(440.0))
                .andExpect(jsonPath("$.grossExpenses").value(150.75))
                .andExpect(jsonPath("$.operatingProfit").value(289.25))
                .andExpect(jsonPath("$.estimatedVatPayable").value(50.20))
                .andExpect(jsonPath("$.estimatedIrpfPayable").value(35.86))
                .andExpect(jsonPath("$.estimatedTotalTaxes").value(86.06))
                .andExpect(jsonPath("$.profitabilityRatio").value(65.74))
                .andExpect(jsonPath("$.operatingProfitChangePercentage").value(nullValue()))
                .andExpect(jsonPath("$.monthlyBreakdown", hasSize(3)))
                .andExpect(jsonPath("$.monthlyBreakdown[0].month").value(1))
                .andExpect(jsonPath("$.monthlyBreakdown[0].grossIncome").value(50.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].grossExpenses").value(20.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].operatingProfit").value(30.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].estimatedVatPayable").value(5.21))
                .andExpect(jsonPath("$.monthlyBreakdown[1].month").value(2))
                .andExpect(jsonPath("$.monthlyBreakdown[1].grossIncome").value(120.0))
                .andExpect(jsonPath("$.monthlyBreakdown[1].grossExpenses").value(40.0))
                .andExpect(jsonPath("$.monthlyBreakdown[1].operatingProfit").value(80.0))
                .andExpect(jsonPath("$.monthlyBreakdown[1].estimatedVatPayable").value(13.89))
                .andExpect(jsonPath("$.monthlyBreakdown[2].month").value(3))
                .andExpect(jsonPath("$.monthlyBreakdown[2].grossIncome").value(270.0))
                .andExpect(jsonPath("$.monthlyBreakdown[2].grossExpenses").value(90.75))
                .andExpect(jsonPath("$.monthlyBreakdown[2].operatingProfit").value(179.25))
                .andExpect(jsonPath("$.monthlyBreakdown[2].estimatedVatPayable").value(31.11))
                .andExpect(jsonPath("$.profitEvolution", hasSize(3)))
                .andExpect(jsonPath("$.profitEvolution[0].month").value(1))
                .andExpect(jsonPath("$.profitEvolution[0].operatingProfit").value(30.0))
                .andExpect(jsonPath("$.profitEvolution[1].month").value(2))
                .andExpect(jsonPath("$.profitEvolution[1].operatingProfit").value(80.0))
                .andExpect(jsonPath("$.profitEvolution[2].month").value(3))
                .andExpect(jsonPath("$.profitEvolution[2].operatingProfit").value(179.25));
    }

    @Test
    void quarterlyDetailedReportShouldReturnZeroedBreakdownForValidQuarterWithoutData() throws Exception {
        mockMvc.perform(get("/api/reports/quarterly/detailed")
                        .param("year", "2026")
                        .param("quarter", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossIncome").value(0.0))
                .andExpect(jsonPath("$.grossExpenses").value(0.0))
                .andExpect(jsonPath("$.operatingProfit").value(0.0))
                .andExpect(jsonPath("$.estimatedVatPayable").value(0.0))
                .andExpect(jsonPath("$.estimatedIrpfPayable").value(0.0))
                .andExpect(jsonPath("$.estimatedTotalTaxes").value(0.0))
                .andExpect(jsonPath("$.profitabilityRatio").value(0.0))
                .andExpect(jsonPath("$.operatingProfitChangePercentage").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown", hasSize(3)))
                .andExpect(jsonPath("$.monthlyBreakdown[0].month").value(7))
                .andExpect(jsonPath("$.monthlyBreakdown[0].grossIncome").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].grossExpenses").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].operatingProfit").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].estimatedVatPayable").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[1].month").value(8))
                .andExpect(jsonPath("$.monthlyBreakdown[2].month").value(9))
                .andExpect(jsonPath("$.profitEvolution", hasSize(3)))
                .andExpect(jsonPath("$.profitEvolution[0].month").value(7))
                .andExpect(jsonPath("$.profitEvolution[0].operatingProfit").value(0.0))
                .andExpect(jsonPath("$.profitEvolution[1].month").value(8))
                .andExpect(jsonPath("$.profitEvolution[2].month").value(9));
    }

    @Test
    void quarterlyDetailedReportShouldReturnBadRequestForInvalidQuarter() throws Exception {
        mockMvc.perform(get("/api/reports/quarterly/detailed")
                        .param("year", "2026")
                        .param("quarter", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void quarterlyDetailedReportShouldReturnBadRequestForInvalidYear() throws Exception {
        mockMvc.perform(get("/api/reports/quarterly/detailed")
                        .param("year", "1900")
                        .param("quarter", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void annualConsolidatedReportShouldReturnYearlyFinancialBreakdownWithTwelveMonths() throws Exception {
        mockMvc.perform(get("/api/reports/annual/consolidated")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossIncome").value(440.0))
                .andExpect(jsonPath("$.grossExpenses").value(150.75))
                .andExpect(jsonPath("$.operatingProfit").value(289.25))
                .andExpect(jsonPath("$.estimatedVatPayable").value(50.20))
                .andExpect(jsonPath("$.estimatedIrpfPayable").value(35.86))
                .andExpect(jsonPath("$.estimatedTotalTaxes").value(86.06))
                .andExpect(jsonPath("$.profitabilityRatio").value(65.74))
                .andExpect(jsonPath("$.operatingProfitChangePercentage").value(nullValue()))
                .andExpect(jsonPath("$.monthlyBreakdown", hasSize(12)))
                .andExpect(jsonPath("$.monthlyBreakdown[0].month").value(1))
                .andExpect(jsonPath("$.monthlyBreakdown[0].monthLabel").value("Enero"))
                .andExpect(jsonPath("$.monthlyBreakdown[0].grossIncome").value(50.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].grossExpenses").value(20.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].operatingProfit").value(30.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].estimatedVatPayable").value(5.21))
                .andExpect(jsonPath("$.monthlyBreakdown[0].estimatedIrpfPayable").value(3.72))
                .andExpect(jsonPath("$.monthlyBreakdown[1].month").value(2))
                .andExpect(jsonPath("$.monthlyBreakdown[1].monthLabel").value("Febrero"))
                .andExpect(jsonPath("$.monthlyBreakdown[1].grossIncome").value(120.0))
                .andExpect(jsonPath("$.monthlyBreakdown[1].grossExpenses").value(40.0))
                .andExpect(jsonPath("$.monthlyBreakdown[1].operatingProfit").value(80.0))
                .andExpect(jsonPath("$.monthlyBreakdown[1].estimatedVatPayable").value(13.89))
                .andExpect(jsonPath("$.monthlyBreakdown[1].estimatedIrpfPayable").value(9.92))
                .andExpect(jsonPath("$.monthlyBreakdown[2].month").value(3))
                .andExpect(jsonPath("$.monthlyBreakdown[2].monthLabel").value("Marzo"))
                .andExpect(jsonPath("$.monthlyBreakdown[2].grossIncome").value(270.0))
                .andExpect(jsonPath("$.monthlyBreakdown[2].grossExpenses").value(90.75))
                .andExpect(jsonPath("$.monthlyBreakdown[2].operatingProfit").value(179.25))
                .andExpect(jsonPath("$.monthlyBreakdown[2].estimatedVatPayable").value(31.11))
                .andExpect(jsonPath("$.monthlyBreakdown[2].estimatedIrpfPayable").value(22.22))
                .andExpect(jsonPath("$.monthlyBreakdown[3].month").value(4))
                .andExpect(jsonPath("$.monthlyBreakdown[3].monthLabel").value("Abril"))
                .andExpect(jsonPath("$.monthlyBreakdown[3].grossIncome").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[3].grossExpenses").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[3].operatingProfit").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[3].estimatedVatPayable").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[3].estimatedIrpfPayable").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[11].month").value(12))
                .andExpect(jsonPath("$.monthlyBreakdown[11].monthLabel").value("Diciembre"))
                .andExpect(jsonPath("$.monthlyBreakdown[11].grossIncome").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[11].grossExpenses").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[11].operatingProfit").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[11].estimatedVatPayable").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[11].estimatedIrpfPayable").value(0.0))
                .andExpect(jsonPath("$.profitEvolution", hasSize(12)))
                .andExpect(jsonPath("$.profitEvolution[0].month").value(1))
                .andExpect(jsonPath("$.profitEvolution[0].monthLabel").value("Enero"))
                .andExpect(jsonPath("$.profitEvolution[0].operatingProfit").value(30.0))
                .andExpect(jsonPath("$.profitEvolution[1].month").value(2))
                .andExpect(jsonPath("$.profitEvolution[1].monthLabel").value("Febrero"))
                .andExpect(jsonPath("$.profitEvolution[1].operatingProfit").value(80.0))
                .andExpect(jsonPath("$.profitEvolution[2].month").value(3))
                .andExpect(jsonPath("$.profitEvolution[2].monthLabel").value("Marzo"))
                .andExpect(jsonPath("$.profitEvolution[2].operatingProfit").value(179.25))
                .andExpect(jsonPath("$.profitEvolution[11].month").value(12))
                .andExpect(jsonPath("$.profitEvolution[11].monthLabel").value("Diciembre"))
                .andExpect(jsonPath("$.profitEvolution[11].operatingProfit").value(0.0));
    }

    @Test
    void annualConsolidatedReportShouldReturnZeroedYearWithTwelveMonthsWhenYearHasNoData() throws Exception {
        mockMvc.perform(get("/api/reports/annual/consolidated")
                        .param("year", "2030"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossIncome").value(0.0))
                .andExpect(jsonPath("$.grossExpenses").value(0.0))
                .andExpect(jsonPath("$.operatingProfit").value(0.0))
                .andExpect(jsonPath("$.estimatedVatPayable").value(0.0))
                .andExpect(jsonPath("$.estimatedIrpfPayable").value(0.0))
                .andExpect(jsonPath("$.estimatedTotalTaxes").value(0.0))
                .andExpect(jsonPath("$.profitabilityRatio").value(0.0))
                .andExpect(jsonPath("$.operatingProfitChangePercentage").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown", hasSize(12)))
                .andExpect(jsonPath("$.monthlyBreakdown[0].month").value(1))
                .andExpect(jsonPath("$.monthlyBreakdown[0].monthLabel").value("Enero"))
                .andExpect(jsonPath("$.monthlyBreakdown[0].grossIncome").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].grossExpenses").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].operatingProfit").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].estimatedVatPayable").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[0].estimatedIrpfPayable").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[11].month").value(12))
                .andExpect(jsonPath("$.monthlyBreakdown[11].monthLabel").value("Diciembre"))
                .andExpect(jsonPath("$.monthlyBreakdown[11].grossIncome").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[11].grossExpenses").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[11].operatingProfit").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[11].estimatedVatPayable").value(0.0))
                .andExpect(jsonPath("$.monthlyBreakdown[11].estimatedIrpfPayable").value(0.0))
                .andExpect(jsonPath("$.profitEvolution", hasSize(12)))
                .andExpect(jsonPath("$.profitEvolution[0].month").value(1))
                .andExpect(jsonPath("$.profitEvolution[0].monthLabel").value("Enero"))
                .andExpect(jsonPath("$.profitEvolution[0].operatingProfit").value(0.0))
                .andExpect(jsonPath("$.profitEvolution[11].month").value(12))
                .andExpect(jsonPath("$.profitEvolution[11].monthLabel").value("Diciembre"))
                .andExpect(jsonPath("$.profitEvolution[11].operatingProfit").value(0.0));
    }

    @Test
    void annualConsolidatedReportShouldReturnBadRequestForInvalidYear() throws Exception {
        mockMvc.perform(get("/api/reports/annual/consolidated")
                        .param("year", "1900"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void annualConsolidatedPdfShouldReturnValidPdfWithAnnualTableFromJanuaryToDecember() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reports/annual/consolidated/pdf")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .contains("informe-anual-consolidado-2026.pdf");

        String pdfText = extractPdfText(pdfBytes);

        assertThat(pdfText).contains("INFORME ANUAL CONSOLIDADO");
        assertThat(pdfText).contains("Resumen financiero anual");
        assertThat(pdfText).contains("Desglose mensual");
        assertThat(pdfText).contains("Total anual");
        assertThat(pdfText).contains("Resumen fiscal estimado anual");
        assertThat(pdfText).contains("TOTAL ESTIMADO A PAGAR");
        assertThat(pdfText).contains("440,00 EUR");
        assertThat(pdfText).contains("150,75 EUR");
        assertThat(pdfText).contains("289,25 EUR");
        assertThat(pdfText).contains("50,20 EUR");
        assertThat(pdfText).contains("35,86 EUR");
        assertThat(pdfText).contains("86,06 EUR");
        assertPdfContainsAllMonths(pdfText);
    }

    @Test
    void annualConsolidatedPdfShouldReturnValidPdfWithZeroAmountsWhenYearHasNoData() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reports/annual/consolidated/pdf")
                        .param("year", "2030"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .contains("informe-anual-consolidado-2030.pdf");

        String pdfText = extractPdfText(pdfBytes);

        assertThat(pdfText).contains("INFORME ANUAL CONSOLIDADO");
        assertThat(pdfText).contains("Resumen financiero anual");
        assertThat(pdfText).contains("Desglose mensual");
        assertThat(pdfText).contains("Total anual");
        assertThat(pdfText).contains("TOTAL ESTIMADO A PAGAR");
        assertPdfContainsAllMonths(pdfText);
        assertThat(countOccurrences(pdfText, "0,00 EUR")).isGreaterThanOrEqualTo(20);
    }

    @Test
    void annualConsolidatedPdfShouldReturnBadRequestForInvalidYear() throws Exception {
        mockMvc.perform(get("/api/reports/annual/consolidated/pdf")
                        .param("year", "1900"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void serviceRankingShouldReturnMarchServicesOrderedAndPaginated() throws Exception {
        mockMvc.perform(get("/api/reports/ranking")
                        .param("type", "SERVICE")
                        .param("year", "2026")
                        .param("month", "3")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("SERVICE"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].position").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Facial Premium"))
                .andExpect(jsonPath("$.content[0].categoryName").value("Servicios Reports"))
                .andExpect(jsonPath("$.content[0].totalCount").value(2))
                .andExpect(jsonPath("$.content[0].generatedIncome").value(100.0))
                .andExpect(jsonPath("$.content[0].averageDurationMinutes").value(60))
                .andExpect(jsonPath("$.content[1].position").value(2))
                .andExpect(jsonPath("$.content[1].name").value("Manicura Express"))
                .andExpect(jsonPath("$.content[1].categoryName").value("Servicios Reports"))
                .andExpect(jsonPath("$.content[1].totalCount").value(2))
                .andExpect(jsonPath("$.content[1].generatedIncome").value(80.0))
                .andExpect(jsonPath("$.content[1].averageDurationMinutes").value(45));
    }

    @Test
    void productRankingShouldReturnMarchProductsOrderedAndPaginated() throws Exception {
        mockMvc.perform(get("/api/reports/ranking")
                        .param("type", "PRODUCT")
                        .param("year", "2026")
                        .param("month", "3")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PRODUCT"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].position").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Serum Pro"))
                .andExpect(jsonPath("$.content[0].categoryName").value("Productos Reports"))
                .andExpect(jsonPath("$.content[0].totalCount").value(3))
                .andExpect(jsonPath("$.content[0].generatedIncome").value(60.0))
                .andExpect(jsonPath("$.content[1].position").value(2))
                .andExpect(jsonPath("$.content[1].name").value("Champu Repair"))
                .andExpect(jsonPath("$.content[1].categoryName").value("Productos Reports"))
                .andExpect(jsonPath("$.content[1].totalCount").value(2))
                .andExpect(jsonPath("$.content[1].generatedIncome").value(30.0));
    }

    @Test
    void monthlyDetailedPdfShouldReturnPdfWithMarchFinancialValues() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reports/monthly/detailed/pdf")
                        .param("year", "2026")
                        .param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .contains("informe-mensual-detallado-2026-03.pdf");

        String pdfText = extractPdfText(pdfBytes);

        assertThat(pdfText).contains("INFORME MENSUAL DETALLADO");
        assertThat(pdfText).contains("Resumen financiero");
        assertThat(pdfText).contains("Ingresos totales");
        assertThat(pdfText).contains("270,00 EUR");
        assertThat(pdfText).contains("Gastos totales");
        assertThat(pdfText).contains("90,75 EUR");
        assertThat(pdfText).contains("Beneficio operativo");
        assertThat(pdfText).contains("179,25 EUR");
        assertThat(pdfText).contains("IVA estimado a pagar");
        assertThat(pdfText).contains("31,11 EUR");
        assertThat(pdfText).contains("IRPF estimado (15%)");
        assertThat(pdfText).contains("22,22 EUR");
        assertThat(pdfText).contains("TOTAL ESTIMADO A PAGAR");
        assertThat(pdfText).contains("53,33 EUR");
    }

    @Test
    void monthlyDetailedPdfShouldReturnValidPdfWithZeroAmountsWhenMonthHasNoData() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reports/monthly/detailed/pdf")
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");

        String pdfText = extractPdfText(pdfBytes);

        assertThat(pdfText).contains("INFORME MENSUAL DETALLADO");
        assertThat(pdfText).contains("Ingresos totales");
        assertThat(pdfText).contains("0,00 EUR");
        assertThat(pdfText).contains("Gastos totales");
        assertThat(pdfText).contains("Beneficio operativo");
        assertThat(pdfText).contains("IVA estimado a pagar");
        assertThat(pdfText).contains("IRPF estimado (15%)");
        assertThat(pdfText).contains("TOTAL ESTIMADO A PAGAR");
    }

    @Test
    void monthlyDetailedPdfShouldReturnBadRequestForInvalidMonth() throws Exception {
        mockMvc.perform(get("/api/reports/monthly/detailed/pdf")
                        .param("year", "2026")
                        .param("month", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void quarterlyDetailedPdfShouldReturnValidPdfWithQuarterlySectionsAndZeroAmountsWhenQuarterHasNoData() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reports/quarterly/detailed/pdf")
                        .param("year", "2026")
                        .param("quarter", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .contains("informe-trimestral-detallado-2026-Q2.pdf");

        String pdfText = extractPdfText(pdfBytes);

        assertThat(pdfText).contains("INFORME TRIMESTRAL DETALLADO");
        assertThat(pdfText).contains("Resumen financiero del trimestre");
        assertThat(pdfText).contains("Ingresos del trimestre");
        assertThat(pdfText).contains("Gastos del trimestre");
        assertThat(pdfText).contains("Beneficio operativo");
        assertThat(pdfText).contains("Desglose mensual del trimestre");
        assertThat(pdfText).contains("Mes");
        assertThat(pdfText).contains("Abril");
        assertThat(pdfText).contains("Mayo");
        assertThat(pdfText).contains("Junio");
        assertThat(pdfText).contains("Total trimestre");
        assertThat(pdfText).contains("Resumen fiscal estimado");
        assertThat(pdfText).contains("IVA estimado a pagar");
        assertThat(pdfText).contains("IRPF estimado (15%)");
        assertThat(pdfText).contains("TOTAL ESTIMADO A PAGAR");
        assertThat(countOccurrences(pdfText, "0,00 EUR")).isGreaterThanOrEqualTo(10);
    }

    @Test
    void quarterlyDetailedPdfShouldReturnBadRequestForInvalidQuarter() throws Exception {
        mockMvc.perform(get("/api/reports/quarterly/detailed/pdf")
                        .param("year", "2026")
                        .param("quarter", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private void seedReportsData() {
        CategoryEntity servicesCategory = saveCategory("Servicios Reports");
        CategoryEntity productsCategory = saveCategory("Productos Reports");

        reportsClient = saveClient("Cliente Reports");
        facialService = saveService("Facial Premium", 60, 50.0, servicesCategory);
        manicureService = saveService("Manicura Express", 45, 40.0, servicesCategory);
        serumProduct = saveProduct("Serum Pro", productsCategory);
        shampooProduct = saveProduct("Champu Repair", productsCategory);

        AppointmentEntity januaryAppointment = saveAppointment(
                facialService,
                LocalDate.of(2026, 1, 5),
                LocalTime.of(10, 0),
                50.0,
                AppointmentStatus.CONFIRMED
        );
        AppointmentEntity februaryAppointment = saveAppointment(
                manicureService,
                LocalDate.of(2026, 2, 10),
                LocalTime.of(11, 0),
                120.0,
                AppointmentStatus.CONFIRMED
        );

        AppointmentEntity marchAppointmentOne = saveAppointment(
                facialService,
                LocalDate.of(2026, 3, 2),
                LocalTime.of(10, 0),
                50.0,
                AppointmentStatus.CONFIRMED
        );
        AppointmentEntity marchAppointmentTwo = saveAppointment(
                facialService,
                LocalDate.of(2026, 3, 9),
                LocalTime.of(11, 0),
                50.0,
                AppointmentStatus.CONFIRMED
        );
        AppointmentEntity marchAppointmentThree = saveAppointment(
                manicureService,
                LocalDate.of(2026, 3, 3),
                LocalTime.of(12, 0),
                40.0,
                AppointmentStatus.CONFIRMED
        );
        AppointmentEntity marchAppointmentFour = saveAppointment(
                manicureService,
                LocalDate.of(2026, 3, 6),
                LocalTime.of(13, 0),
                40.0,
                AppointmentStatus.CONFIRMED
        );

        saveAppointment(
                facialService,
                LocalDate.of(2026, 3, 4),
                LocalTime.of(14, 0),
                50.0,
                AppointmentStatus.PENDING
        );

        saveServiceSale(januaryAppointment, facialService, new BigDecimal("50.00"));
        saveServiceSale(februaryAppointment, manicureService, new BigDecimal("120.00"));
        saveServiceSale(marchAppointmentOne, facialService, new BigDecimal("50.00"));
        saveServiceSale(marchAppointmentTwo, facialService, new BigDecimal("50.00"));
        saveServiceSale(marchAppointmentThree, manicureService, new BigDecimal("40.00"));
        saveServiceSale(marchAppointmentFour, manicureService, new BigDecimal("40.00"));

        saveProductSale(serumProduct, 3, new BigDecimal("20.00"), LocalDate.of(2026, 3, 10));
        saveProductSale(shampooProduct, 2, new BigDecimal("15.00"), LocalDate.of(2026, 3, 11));

        saveExpense("Alquiler enero", new BigDecimal("20.00"), LocalDate.of(2026, 1, 7));
        saveExpense("Suministros febrero", new BigDecimal("40.00"), LocalDate.of(2026, 2, 12));
        saveExpense("Nominas marzo", new BigDecimal("60.75"), LocalDate.of(2026, 3, 8));
        saveExpense("Marketing marzo", new BigDecimal("30.00"), LocalDate.of(2026, 3, 15));
    }

    private CategoryEntity saveCategory(String name) {
        return categoryRepository.saveAndFlush(
                CategoryEntity.builder()
                        .name(name)
                        .color("blue")
                        .icon("tag")
                        .build()
        );
    }

    private ClientEntity saveClient(String fullName) {
        ClientEntity client = new ClientEntity();
        client.setFullName(fullName);
        client.setPhone("600111222");
        client.setEmail("reports@test.com");
        return clientRepository.saveAndFlush(client);
    }

    private ServiceEntity saveService(String name, int durationMinutes, double price, CategoryEntity category) {
        return serviceRepository.saveAndFlush(
                ServiceEntity.builder()
                        .name(name)
                        .description("Servicio para reports")
                        .durationMinutes(durationMinutes)
                        .price(price)
                        .active(true)
                        .category(category)
                        .build()
        );
    }

    private InventoryProductEntity saveProduct(String name, CategoryEntity category) {
        return inventoryProductRepository.saveAndFlush(
                InventoryProductEntity.builder()
                        .name(name)
                        .description("Producto para reports")
                        .category(category)
                        .stock(20)
                        .minimumStock(5)
                        .sku("SKU-" + name.replace(" ", "-"))
                        .purchasePrice(new BigDecimal("10.00"))
                        .active(true)
                        .build()
        );
    }

    private AppointmentEntity saveAppointment(
            ServiceEntity service,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            double price,
            AppointmentStatus status
    ) {
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setClientId(reportsClient.getId());
        appointment.setClientName(reportsClient.getFullName());
        appointment.setServiceId(service.getId());
        appointment.setServiceName(service.getName());
        appointment.setPrice(price);
        appointment.setProfessionalName("Laura");
        appointment.setAppointmentDate(appointmentDate);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setStatus(status);
        appointment.setInternalNotes("Dataset reports");
        return appointmentRepository.saveAndFlush(appointment);
    }

    private void saveServiceSale(AppointmentEntity appointment, ServiceEntity service, BigDecimal totalAmount) {
        saleRepository.saveAndFlush(
                SaleEntity.builder()
                        .sourceType(SaleSourceType.APPOINTMENT)
                        .appointmentId(appointment.getId())
                        .client(reportsClient)
                        .clientName(reportsClient.getFullName())
                        .itemType(SaleItemType.SERVICE)
                        .service(service)
                        .itemName(service.getName())
                        .unitPrice(totalAmount)
                        .quantity(1)
                        .totalAmount(totalAmount)
                        .saleDate(appointment.getAppointmentDate())
                        .notes("Venta automatica de prueba")
                        .build()
        );
    }

    private void saveProductSale(
            InventoryProductEntity product,
            int quantity,
            BigDecimal unitPrice,
            LocalDate saleDate
    ) {
        saleRepository.saveAndFlush(
                SaleEntity.builder()
                        .sourceType(SaleSourceType.MANUAL)
                        .client(reportsClient)
                        .clientName(reportsClient.getFullName())
                        .itemType(SaleItemType.PRODUCT)
                        .inventoryProduct(product)
                        .itemName(product.getName())
                        .unitPrice(unitPrice)
                        .quantity(quantity)
                        .totalAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                        .saleDate(saleDate)
                        .notes("Venta manual de prueba")
                        .build()
        );
    }

    private void saveExpense(String concept, BigDecimal amount, LocalDate expenseDate) {
        expenseRepository.saveAndFlush(
                ExpenseEntity.builder()
                        .expenseDate(expenseDate)
                        .concept(concept)
                        .category(ExpenseCategory.OTHER)
                        .quantity(1)
                        .amount(amount)
                        .notes("Gasto para reports")
                        .stockMovementApplied(false)
                        .build()
        );
    }

    private String extractPdfText(byte[] pdfBytes) throws Exception {
        PdfReader reader = new PdfReader(pdfBytes);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return normalizePdfText(text.toString());
        } finally {
            reader.close();
        }
    }

    private String normalizePdfText(String text) {
        return text
                .replace('\u00A0', ' ')
                .replace("€", "EUR")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void assertPdfContainsAllMonths(String pdfText) {
        for (String month : new String[]{
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        }) {
            assertThat(pdfText).contains(month);
        }
    }

    private int countOccurrences(String text, String value) {
        return text.split(java.util.regex.Pattern.quote(value), -1).length - 1;
    }
}
