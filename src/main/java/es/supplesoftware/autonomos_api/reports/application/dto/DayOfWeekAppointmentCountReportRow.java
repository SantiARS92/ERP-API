package es.supplesoftware.autonomos_api.reports.application.dto;
import java.time.DayOfWeek;

public record DayOfWeekAppointmentCountReportRow(
        DayOfWeek dayOfWeek,
        long confirmedAppointments
) {
}
