package no.difi.meldingsutveksling.oauth2;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MaskinportenTokenInterceptor implements ClientHttpRequestInterceptor {

    private final GetMaskinportenToken getMaskinportenToken;

    public MaskinportenTokenInterceptor(IntegrasjonspunktProperties props) {
        this.getMaskinportenToken = new GetMaskinportenToken(props);
    }

    @NonNull
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte @NonNull [] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().setBearerAuth(getMaskinportenToken.getMaskinportenToken());
        return execution.execute(request, body);
    }
}
