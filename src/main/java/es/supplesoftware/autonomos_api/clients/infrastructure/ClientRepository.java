package es.supplesoftware.autonomos_api.clients.infrastructure;
import es.supplesoftware.autonomos_api.clients.infrastructure.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;


public interface ClientRepository extends JpaRepository<ClientEntity, Long>, JpaSpecificationExecutor<ClientEntity> {
    Optional<ClientEntity> findByFullNameIgnoreCase(String fullName);

    @Query(value = "select nextval('clients_identificador_seq')", nativeQuery = true)
    Long nextIdentificador();
}
