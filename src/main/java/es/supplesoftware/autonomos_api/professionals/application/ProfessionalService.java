package es.supplesoftware.autonomos_api.professionals.application;
import es.supplesoftware.autonomos_api.professionals.infrastructure.ProfessionalEntity;
import es.supplesoftware.autonomos_api.professionals.infrastructure.ProfessionalRepository;
import es.supplesoftware.autonomos_api.professionals.application.dto.ProfessionalRequest;
import es.supplesoftware.autonomos_api.professionals.application.dto.ProfessionalResponse;
import es.supplesoftware.autonomos_api.common.exception.BadRequestException;
import es.supplesoftware.autonomos_api.common.exception.ConflictException;
import es.supplesoftware.autonomos_api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<ProfessionalResponse> getAll(Boolean active) {
        List<ProfessionalEntity> professionals = active == null
                ? professionalRepository.findAllByOrderByFullNameAsc()
                : professionalRepository.findByActiveOrderByFullNameAsc(active);

        return professionals.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfessionalResponse getById(Long id) {
        ProfessionalEntity professional = professionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado con id: " + id));

        return mapToResponse(professional);
    }

    public ProfessionalResponse create(ProfessionalRequest request) {
        ProfessionalEntity professional = new ProfessionalEntity();
        applyRequest(professional, request);
        professional.setPasswordHash(requireEncodedAccessPassword(request.getAccessPassword()));

        ProfessionalEntity saved = professionalRepository.save(professional);
        return mapToResponse(saved);
    }

    public ProfessionalResponse update(Long id, ProfessionalRequest request) {
        ProfessionalEntity professional = professionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado con id: " + id));

        applyRequest(professional, request);
        updatePasswordIfProvided(professional, request.getAccessPassword());

        ProfessionalEntity saved = professionalRepository.save(professional);
        return mapToResponse(saved);
    }

    private void applyRequest(ProfessionalEntity entity, ProfessionalRequest request) {
        String normalizedFullName = request.getFullName().trim();
        validateFullNameUniqueness(normalizedFullName, entity.getId());
        validateReservedFullName(normalizedFullName);

        entity.setFullName(normalizedFullName);
        entity.setSpecialty(request.getSpecialty().trim());
        entity.setPhone(blankToNull(request.getPhone()));
        entity.setEmail(blankToNull(request.getEmail()));
        entity.setActive(request.getActive());
    }

    private ProfessionalResponse mapToResponse(ProfessionalEntity entity) {
        return new ProfessionalResponse(
                entity.getId(),
                entity.getFullName(),
                entity.getSpecialty(),
                entity.getPhone(),
                entity.getEmail(),
                StringUtils.hasText(entity.getPasswordHash()),
                entity.getActive()
        );
    }

    private void validateFullNameUniqueness(String fullName, Long currentId) {
        professionalRepository.findByNormalizedFullName(fullName)
                .ifPresent(existingProfessional -> {
                    if (currentId == null || !existingProfessional.getId().equals(currentId)) {
                        throw new ConflictException("Ya existe un profesional con ese nombre completo");
                    }
                });
    }

    private String requireEncodedAccessPassword(String accessPassword) {
        String normalizedAccessPassword = blankToNull(accessPassword);
        if (normalizedAccessPassword == null) {
            throw new BadRequestException("La clave de acceso del profesional es obligatoria");
        }

        return passwordEncoder.encode(normalizedAccessPassword);
    }

    private void updatePasswordIfProvided(ProfessionalEntity professional, String accessPassword) {
        String normalizedAccessPassword = blankToNull(accessPassword);
        if (normalizedAccessPassword == null) {
            return;
        }

        professional.setPasswordHash(passwordEncoder.encode(normalizedAccessPassword));
    }

    private void validateReservedFullName(String fullName) {
        if ("admin".equalsIgnoreCase(fullName.trim()) || "guest".equalsIgnoreCase(fullName.trim())) {
            throw new ConflictException("Ese nombre no puede usarse como profesional");
        }
    }
    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
