package no.difi.meldingsutveksling.nextmove.nhn;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;

public class NhnAdapterClient {

    RestClient restClient;

    public void sendIn(HttpServletRequest request) {
        restClient.method(HttpMethod.POST).uri("myserviceUrl").body(request.getB)
    }
}
