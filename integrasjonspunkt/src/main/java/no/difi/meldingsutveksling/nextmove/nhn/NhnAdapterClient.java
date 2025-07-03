package no.difi.meldingsutveksling.nextmove.nhn;

import com.azure.core.http.rest.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    public void messageOut(DPHMessageOut messageOut) {

        ResponseEntity<Void> response = dphClient.method(HttpMethod.POST).uri(uri).body(messageOut).retrieve().toEntity(Void.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("DPH message out NOT OK " + response.getStatusCode());
        }


    }
}
