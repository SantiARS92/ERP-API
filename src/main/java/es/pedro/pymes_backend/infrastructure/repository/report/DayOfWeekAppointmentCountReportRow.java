package es.pedro.pymes_backend.infrastructure.repository.report;

import java.time.DayOfWeek;

public record DayOfWeekAppointmentCountReportRow(
        DayOfWeek dayOfWeek,
        long confirmedAppointments
) {
}
