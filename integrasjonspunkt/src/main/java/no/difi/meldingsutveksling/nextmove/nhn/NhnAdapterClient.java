package no.difi.meldingsutveksling.nextmove.nhn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.EncryptedBusinessMessage;
import no.difi.meldingsutveksling.exceptions.CanNotRetrieveHealthcareStatusException;
import no.difi.meldingsutveksling.jpa.ObjectMapperHolder;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nhn.adapter.crypto.EncryptionException;
import no.difi.meldingsutveksling.nhn.adapter.crypto.NhnKeystore;
import no.difi.meldingsutveksling.nhn.adapter.crypto.Signer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class NhnAdapterClient {

    private RestClient dphClient;

    private final String uri;
    private final String MESSAGE_OUT_PATH ;
    private final String MESSAGE_RECEIPT_PATH;
    private final String ON_BEHALF_OF_PARAM = "onBehalfOf";
    private final String MESSAGE_STATUS_PATH;
    private final Signer signer;
    private final NhnKeystore keystore;
    private final BusinessMessageEncryptionService businessMessageEncryptionService;



    public NhnAdapterClient(RestClient dphClient, @Value("${difi.move.dph.adapter.url}") String uri, Signer signer, NhnKeystore keystore, BusinessMessageEncryptionService businessMessageEncryptionService) {
        this.signer = signer;
        this.keystore = keystore;
        this.businessMessageEncryptionService = businessMessageEncryptionService;
        log.info("adapter URL is {}", uri);
        this.dphClient = dphClient;
        this.uri = uri;

        this.MESSAGE_OUT_PATH = uri + "/out";
        this.MESSAGE_RECEIPT_PATH = uri + "/in/%s/receipt";
        this.MESSAGE_STATUS_PATH = uri + "/status/%s";
    }

    public String messageOut(DPHMessageOut messageOut) {
        final String signedJson;
        try {

            String rawJson = ObjectMapperHolder.get().writeValueAsString(messageOut);

            signedJson = signer.sign(rawJson);

        } catch (JsonProcessingException e) {
            throw new NextMoveRuntimeException("Failed to serialize DPHMessageOut to JSON for signing", e);
        } catch (Exception e) {
            throw new NextMoveRuntimeException("Failed to sign DPHMessageOut JSON", e);
        }

        return dphClient.method(HttpMethod.POST)
            .uri(MESSAGE_OUT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .body(signedJson)
            .retrieve()
            .onStatus(t -> t.equals(HttpStatus.BAD_REQUEST),
                (request, resp) -> {
                    throw new NextMoveRuntimeException(resp.getStatusText());
                })
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                throw new NextMoveRuntimeException(resp.getStatusText());
            })
            .onStatus(HttpStatusCode::is5xxServerError, ((request, response) -> {
                throw new NextMoveRuntimeException("Server error");
            })).toEntity(String.class).getBody();
    }


    public DPHMessageStatus messageStatus(UUID messageReference, String onBehalfOf) {
        return dphClient.method(HttpMethod.GET).uri(MESSAGE_STATUS_PATH.formatted(messageReference)  + "?onBehalfOf=" + onBehalfOf).retrieve().toEntity(DPHMessageStatus.class).getBody();
    }


    public List<IncomingReceipt> messageReceipt(UUID messageReference, String onBehalfOf) throws no.difi.meldingsutveksling.nhn.adapter.crypto.EncryptionException {

        String kid = keystore.getKidByOrgnummer(onBehalfOf);
        var encryptedReiepts = dphClient.method(HttpMethod.GET)
            .uri(MESSAGE_RECEIPT_PATH.formatted(messageReference) + "?" + ON_BEHALF_OF_PARAM + "=" + onBehalfOf + "&kid=" + kid)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                JsonNode errorBody = ObjectMapperHolder.get().readTree(resp.getBody());
                String error = errorBody.get("error").asText();
                String stackTrace = errorBody.get("stackTrace").asText();
                log.error("error while retriving receipt stacktrace:{}", stackTrace);
                throw new CanNotRetrieveHealthcareStatusException(HttpStatus.BAD_REQUEST, error);
            })
            .toEntity(new ParameterizedTypeReference<List<EncryptedBusinessMessage>>() {
            }).getBody();
        List<IncomingReceipt> result = new ArrayList<>();
        try {
        for (EncryptedBusinessMessage t : encryptedReiepts) {
            result.add(ObjectMapperHolder.get().readValue( businessMessageEncryptionService.decrypt(t),IncomingReceipt.class));
        }
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new NextMoveRuntimeException("Not able to parse incoming receipt", e);
        }
        return result;
    }
}
