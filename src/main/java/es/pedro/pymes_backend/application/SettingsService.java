package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.BusinessClosureEntity;
import es.pedro.pymes_backend.domain.BusinessOpeningHoursEntity;
import es.pedro.pymes_backend.domain.BusinessSettingsEntity;
import es.pedro.pymes_backend.infrastructure.repository.BusinessSettingsRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.BusinessClosureDto;
import es.pedro.pymes_backend.infrastructure.web.dto.BusinessOpeningHoursDto;
import es.pedro.pymes_backend.infrastructure.web.dto.BusinessSettingsRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.BusinessSettingsResponse;
import es.pedro.pymes_backend.infrastructure.web.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class SettingsService {

    private final BusinessSettingsRepository businessSettingsRepository;

    public BusinessSettingsResponse getSettings() {
        BusinessSettingsEntity settings = getOrCreateSettings();
        return mapToResponse(settings);
    }

    public BusinessSettingsResponse updateSettings(BusinessSettingsRequest request) {
        validateOpeningHours(request.getOpeningHours());
        validateSpecialClosures(request.getSpecialClosures());

        BusinessSettingsEntity settings = getOrCreateSettings();

        settings.setBusinessName(request.getBusinessName().trim());
        settings.setPhone(request.getPhone().trim());
        settings.setAddress(request.getAddress().trim());
        settings.setContactEmail(request.getContactEmail().trim());
        settings.setOnlineBookingEnabled(request.getOnlineBookingEnabled());
        settings.setDefaultAppointmentDurationMinutes(request.getDefaultAppointmentDurationMinutes());
        settings.setMinimumAdvanceMinutes(request.getMinimumAdvanceMinutes());
        settings.setMaximumAdvanceDays(request.getMaximumAdvanceDays());
        settings.setFreeCancellationHours(request.getFreeCancellationHours());
        settings.setCancellationPolicy(blankToNull(request.getCancellationPolicy()));

        settings.getOpeningHours().clear();
        settings.getSpecialClosures().clear();
        businessSettingsRepository.flush();

        request.getOpeningHours().stream()
                .sorted(Comparator.comparingInt(item -> item.getDayOfWeek().getValue()))
                .forEach(item -> settings.getOpeningHours().add(mapOpeningHoursEntity(settings, item)));

        request.getSpecialClosures().stream()
                .sorted(Comparator.comparing(BusinessClosureDto::getClosureDate))
                .forEach(item -> settings.getSpecialClosures().add(mapClosureEntity(settings, item)));

        BusinessSettingsEntity saved = businessSettingsRepository.save(settings);
        return mapToResponse(saved);
    }

    private BusinessSettingsEntity getOrCreateSettings() {
        return businessSettingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> businessSettingsRepository.save(createDefaultSettings()));
    }

    private BusinessSettingsEntity createDefaultSettings() {
        BusinessSettingsEntity settings = new BusinessSettingsEntity();
        settings.setBusinessName("");
        settings.setPhone("");
        settings.setAddress("");
        settings.setContactEmail("");
        settings.setOnlineBookingEnabled(false);
        settings.setDefaultAppointmentDurationMinutes(30);
        settings.setMinimumAdvanceMinutes(120);
        settings.setMaximumAdvanceDays(90);
        settings.setFreeCancellationHours(24);
        settings.setCancellationPolicy(null);
        settings.setOpeningHours(new ArrayList<>());
        settings.setSpecialClosures(new ArrayList<>());

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            BusinessOpeningHoursEntity openingHours = new BusinessOpeningHoursEntity();
            openingHours.setSettings(settings);
            openingHours.setDayOfWeek(dayOfWeek);
            openingHours.setEnabled(false);
            openingHours.setOpeningTime(null);
            openingHours.setClosingTime(null);
            settings.getOpeningHours().add(openingHours);
        }

        return settings;
    }

    private void validateOpeningHours(List<BusinessOpeningHoursDto> openingHours) {
        if (openingHours == null || openingHours.size() != 7) {
            throw new BadRequestException("El horario semanal debe contener exactamente 7 días");
        }

        Set<DayOfWeek> uniqueDays = EnumSet.noneOf(DayOfWeek.class);

        for (BusinessOpeningHoursDto item : openingHours) {
            if (!uniqueDays.add(item.getDayOfWeek())) {
                throw new BadRequestException("No se puede repetir un día en el horario semanal");
            }

            if (Boolean.TRUE.equals(item.getEnabled())) {
                if (item.getOpeningTime() == null || item.getClosingTime() == null) {
                    throw new BadRequestException("Los días habilitados deben tener hora de apertura y cierre");
                }

                if (!item.getOpeningTime().isBefore(item.getClosingTime())) {
                    throw new BadRequestException("La hora de apertura debe ser anterior a la hora de cierre");
                }
            }
        }
    }

    private void validateSpecialClosures(List<BusinessClosureDto> specialClosures) {
        Set<java.time.LocalDate> uniqueDates = new HashSet<>();

        for (BusinessClosureDto item : specialClosures) {
            if (!uniqueDates.add(item.getClosureDate())) {
                throw new BadRequestException("No se puede repetir la misma fecha en cierres especiales");
            }
        }
    }

    private BusinessOpeningHoursEntity mapOpeningHoursEntity(BusinessSettingsEntity settings, BusinessOpeningHoursDto dto) {
        BusinessOpeningHoursEntity entity = new BusinessOpeningHoursEntity();
        entity.setSettings(settings);
        entity.setDayOfWeek(dto.getDayOfWeek());
        entity.setEnabled(dto.getEnabled());

        if (Boolean.TRUE.equals(dto.getEnabled())) {
            entity.setOpeningTime(dto.getOpeningTime());
            entity.setClosingTime(dto.getClosingTime());
        } else {
            entity.setOpeningTime(null);
            entity.setClosingTime(null);
        }

        return entity;
    }

    private BusinessClosureEntity mapClosureEntity(BusinessSettingsEntity settings, BusinessClosureDto dto) {
        BusinessClosureEntity entity = new BusinessClosureEntity();
        entity.setSettings(settings);
        entity.setClosureDate(dto.getClosureDate());
        entity.setReason(blankToNull(dto.getReason()));
        return entity;
    }

    private BusinessSettingsResponse mapToResponse(BusinessSettingsEntity settings) {
        List<BusinessOpeningHoursDto> openingHours = settings.getOpeningHours().stream()
                .sorted(Comparator.comparingInt(item -> item.getDayOfWeek().getValue()))
                .map(item -> new BusinessOpeningHoursDto(
                        item.getDayOfWeek(),
                        item.getEnabled(),
                        item.getOpeningTime(),
                        item.getClosingTime()
                ))
                .toList();

        List<BusinessClosureDto> specialClosures = settings.getSpecialClosures().stream()
                .sorted(Comparator.comparing(BusinessClosureEntity::getClosureDate))
                .map(item -> new BusinessClosureDto(
                        item.getClosureDate(),
                        item.getReason()
                ))
                .toList();

        return new BusinessSettingsResponse(
                settings.getBusinessName(),
                settings.getPhone(),
                settings.getAddress(),
                settings.getContactEmail(),
                settings.getOnlineBookingEnabled(),
                settings.getDefaultAppointmentDurationMinutes(),
                settings.getMinimumAdvanceMinutes(),
                settings.getMaximumAdvanceDays(),
                settings.getFreeCancellationHours(),
                settings.getCancellationPolicy(),
                openingHours,
                specialClosures
        );
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
