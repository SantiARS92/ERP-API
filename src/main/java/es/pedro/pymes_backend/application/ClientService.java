package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.ClientEntity;
import es.pedro.pymes_backend.infrastructure.repository.ClientRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.ClientRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.ClientResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.PagedResponse;
import es.pedro.pymes_backend.infrastructure.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    public PagedResponse<ClientResponse> getAllClients(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<ClientEntity> clientPage = clientRepository.findAll(
                ClientSpecification.hasSearch(search),
                pageable
        );

        Page<ClientResponse> responsePage = clientPage.map(this::mapToResponse);

        return new PagedResponse<>(
                responsePage.getContent(),
                responsePage.getNumber(),
                responsePage.getSize(),
                responsePage.getTotalElements(),
                responsePage.getTotalPages(),
                responsePage.isFirst(),
                responsePage.isLast()
        );
    }

    public ClientResponse getClientById(Long id) {
        ClientEntity client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        return mapToResponse(client);
    }

    public ClientResponse createClient(ClientRequest request) {
        ClientEntity client = new ClientEntity();
        client.setFullName(request.getFullName().trim());
        client.setPhone(request.getPhone().trim());
        client.setEmail(normalizeEmail(request.getEmail()));

        ClientEntity savedClient = clientRepository.save(client);
        return mapToResponse(savedClient);
    }

    public ClientResponse updateClient(Long id, ClientRequest request) {
        ClientEntity client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        client.setFullName(request.getFullName().trim());
        client.setPhone(request.getPhone().trim());
        client.setEmail(normalizeEmail(request.getEmail()));

        ClientEntity updatedClient = clientRepository.save(client);
        return mapToResponse(updatedClient);
    }

    public void deleteClient(Long id) {
        ClientEntity client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        clientRepository.delete(client);
    }

    private ClientResponse mapToResponse(ClientEntity client) {
        return new ClientResponse(
                client.getId(),
                client.getFullName(),
                client.getPhone(),
                client.getEmail()
        );
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.trim();
    }
}