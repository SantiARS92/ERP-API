package es.supplesoftware.autonomos_api.clients.application.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClientResponse {

    private Long id;
    private String fullName;
    private String phone;
    private String email;

    @JsonProperty("admiteFacturas")
    private Boolean acceptsInvoices;

    @JsonProperty("direccion")
    private String address;

    private String cifNif;
    private Long identificador;
}
