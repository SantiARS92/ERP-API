package es.supplesoftware.autonomos_api.settings.infrastructure;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "business_closures",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"settings_id", "closure_date"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessClosureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settings_id", nullable = false)
    private BusinessSettingsEntity settings;

    @Column(name = "closure_date", nullable = false)
    private LocalDate closureDate;

    @Column(length = 255)
    private String reason;
}