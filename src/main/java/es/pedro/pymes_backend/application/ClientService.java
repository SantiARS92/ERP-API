package es.pedro.pymes_backend.application;

import es.pedro.pymes_backend.domain.ClientEntity;
import es.pedro.pymes_backend.infrastructure.repository.ClientRepository;
import es.pedro.pymes_backend.infrastructure.web.dto.ClientRequest;
import es.pedro.pymes_backend.infrastructure.web.dto.ClientResponse;
import es.pedro.pymes_backend.infrastructure.web.dto.PagedResponse;
import es.pedro.pymes_backend.infrastructure.web.exception.BadRequestException;
import es.pedro.pymes_backend.infrastructure.web.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import es.pedro.pymes_backend.infrastructure.repository.SaleRepository;
import es.pedro.pymes_backend.infrastructure.web.exception.ConflictException;

@Service
@Transactional
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final SaleRepository saleRepository;
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
        applyRequest(client, request);

        ClientEntity savedClient = clientRepository.save(client);
        return mapToResponse(savedClient);
    }

    public ClientResponse updateClient(Long id, ClientRequest request) {
        ClientEntity client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        applyRequest(client, request);

        ClientEntity updatedClient = clientRepository.save(client);
        return mapToResponse(updatedClient);
    }

    public void deleteClient(Long id) {
        ClientEntity client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        if (saleRepository.existsByClientId(id)) {
            throw new ConflictException("No se puede eliminar el cliente porque tiene ventas asociadas.");
        }
        clientRepository.delete(client);
    }

    private ClientResponse mapToResponse(ClientEntity client) {
        return new ClientResponse(
                client.getId(),
                client.getFullName(),
                client.getPhone(),
                client.getEmail(),
                Boolean.TRUE.equals(client.getAcceptsInvoices()),
                client.getAddress(),
                client.getCifNif(),
                client.getIdentificador()
        );
    }

    private void applyRequest(ClientEntity client, ClientRequest request) {
        client.setFullName(request.getFullName().trim());
        client.setPhone(request.getPhone().trim());
        client.setEmail(normalizeEmail(request.getEmail()));

        boolean acceptsInvoices = Boolean.TRUE.equals(request.getAcceptsInvoices());
        client.setAcceptsInvoices(acceptsInvoices);

        if (!acceptsInvoices) {
            client.setAddress(null);
            client.setCifNif(null);
            client.setIdentificador(null);
            return;
        }

        client.setAddress(requireText(request.getAddress(), "La direccion es obligatoria si el cliente admite facturas"));
        client.setCifNif(requireText(request.getCifNif(), "El CIF/NIF es obligatorio si el cliente admite facturas"));

        if (client.getIdentificador() == null) {
            client.setIdentificador(clientRepository.nextIdentificador());
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.trim();
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }

        return value.trim();
    }
}
