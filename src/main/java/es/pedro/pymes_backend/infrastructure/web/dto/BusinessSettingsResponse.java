package es.pedro.pymes_backend.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessSettingsResponse {

    private String businessName;
    private String phone;
    private String address;
    private String contactEmail;

    private Boolean onlineBookingEnabled;
    private Integer defaultAppointmentDurationMinutes;
    private Integer minimumAdvanceMinutes;
    private Integer maximumAdvanceDays;
    private Integer freeCancellationHours;
    private String cancellationPolicy;

    private List<BusinessOpeningHoursDto> openingHours;
    private List<BusinessClosureDto> specialClosures;
}