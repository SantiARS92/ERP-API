package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.ProfessionalEntity;
import es.pedro.pymes_backend.infrastructure.repository.ProfessionalRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.ProfessionalRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.ProfessionalResponse;
import es.pedro.pymes_backend.infrastructure.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;

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

        ProfessionalEntity saved = professionalRepository.save(professional);
        return mapToResponse(saved);
    }

    public ProfessionalResponse update(Long id, ProfessionalRequest request) {
        ProfessionalEntity professional = professionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado con id: " + id));

        applyRequest(professional, request);

        ProfessionalEntity saved = professionalRepository.save(professional);
        return mapToResponse(saved);
    }

    private void applyRequest(ProfessionalEntity entity, ProfessionalRequest request) {
        entity.setFullName(request.getFullName().trim());
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
                entity.getActive()
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