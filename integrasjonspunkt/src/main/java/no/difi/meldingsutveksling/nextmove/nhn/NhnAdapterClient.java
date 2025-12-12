package no.difi.meldingsutveksling.nextmove.nhn;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.exceptions.CanNotRetrieveHealthcareStatusException;
import no.difi.meldingsutveksling.jpa.ObjectMapperHolder;
import no.difi.meldingsutveksling.nextmove.NextMoveClientInputException;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class NhnAdapterClient {

    private RestClient dphClient;

    private String uri;
    private String MESSAGE_OUT_PATH = uri + "/out";
    private String MESSAGE_RECEIPT_PATH = uri + "/in/%s/receipt";
    private String ON_BEHALF_OF_PARAM = "onBehalfOf";
    private String MESSAGE_STATUS_PATH = uri + "/status/%s";


    public NhnAdapterClient(RestClient dphClient, @Value("${difi.move.dph.adapter.url}") String uri) {
        log.info("adapter URL is " + uri);
        this.dphClient = dphClient;
        this.uri = uri;
    }
    // bare internal bruk
    public String messageOut(DPHMessageOut messageOut) {
        return dphClient.method(HttpMethod.POST)
            .uri(MESSAGE_OUT_PATH)
            .body(messageOut)
            .retrieve()
            .onStatus(t -> t.equals(HttpStatus.BAD_REQUEST),
                (request, resp) -> {
                    throw new NextMoveClientInputException(resp.getStatusText());
                })
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {

                throw new NextMoveClientInputException(resp.getStatusText());
            })
            .onStatus(HttpStatusCode::is5xxServerError, ((request, response) -> {
                throw new NextMoveRuntimeException("Server error");
            })).toEntity(String.class).getBody();
    }

    //bare internal bruk
    public DPHMessageStatus messageStatus(UUID messageReference, String onBehalfOf) {
        return dphClient.method(HttpMethod.GET).uri(MESSAGE_STATUS_PATH.formatted(messageReference)  + "?onBehalfOf=" + onBehalfOf).retrieve().toEntity(DPHMessageStatus.class).getBody();
    }

    //extern bruk
    public List<IncomingReceipt> messageReceipt(UUID messageReference, String onBehalfOf) {

        return dphClient.method(HttpMethod.GET)
            .uri(MESSAGE_RECEIPT_PATH.formatted(messageReference) + "?" + ON_BEHALF_OF_PARAM+"=" + onBehalfOf)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError,(request, resp) -> {
                JsonNode errorBody = ObjectMapperHolder.get().readTree(resp.getBody());
                String error = errorBody.get("error").asText();
                String stackTrace = errorBody.get("stackTrace").asText();
                log.error("error while retriving receipt stacktrace:{}", stackTrace);
                throw new CanNotRetrieveHealthcareStatusException(HttpStatus.BAD_REQUEST, error);
            })
            .toEntity(new ParameterizedTypeReference<List<IncomingReceipt>>() {
        }).getBody();
    }
}
