package no.difi.meldingsutveksling.nextmove.nhn;

import com.azure.core.http.rest.Response;
import jakarta.servlet.http.HttpServletRequest;
import kotlin.uuid.Uuid;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class NhnAdapterClient {

    private RestClient dphClient;

    private String uri;

    public NhnAdapterClient(RestClient dphClient,@Value("${difi.move.dph.adapter.url}") String uri) {
        log.info("adapter URL is " + uri);
        this.dphClient = dphClient;
        this.uri = uri;
    }

    public String messageOut(DPHMessageOut messageOut) {

        ResponseEntity<String> response = dphClient.method(HttpMethod.POST).uri(uri + "/out").body(messageOut).retrieve().toEntity(String.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("DPH message out NOT OK " + response.getStatusCode());
            throw new NextMoveRuntimeException("Error during innsending av DPH message with id " + messageOut.messageId());
        }

        return response.getBody();
    }

    public DPHMessageStatus messageStatus(UUID messageReference, String onBehalfOf) {
        return dphClient.method(HttpMethod.GET).uri(uri + "/status/" + messageReference + "?onBehalfOf=" + onBehalfOf).retrieve().toEntity(DPHMessageStatus.class).getBody();
    }

    public List<IncomingReceipt> messageReceipt(UUID messageReference, String onBehalfOf) {

       return dphClient.method(HttpMethod.GET).uri(uri + "/in/" + messageReference.toString() +"/receipt" + "?onBehalfOf=" + onBehalfOf).retrieve().toEntity(new ParameterizedTypeReference<List<IncomingReceipt>>() {
       }).getBody();
    }
}
