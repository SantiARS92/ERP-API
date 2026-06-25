package es.supplesoftware.autonomos_api.settings.infrastructure;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "business_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String businessName;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 150)
    private String contactEmail;

    @Column(nullable = false)
    private Boolean onlineBookingEnabled = false;

    @Column(nullable = false)
    private Integer defaultAppointmentDurationMinutes = 30;

    @Column(nullable = false)
    private Integer minimumAdvanceMinutes = 120;

    @Column(nullable = false)
    private Integer maximumAdvanceDays = 90;

    @Column(nullable = false)
    private Integer freeCancellationHours = 24;

    @Column(columnDefinition = "TEXT")
    private String cancellationPolicy;

    @OneToMany(mappedBy = "settings", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BusinessOpeningHoursEntity> openingHours = new ArrayList<>();

    @OneToMany(mappedBy = "settings", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BusinessClosureEntity> specialClosures = new ArrayList<>();
}