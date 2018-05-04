package no.difi.meldingsutveksling.nextmove;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;

@Slf4j
public class ServiceBusRestErrorHandler extends DefaultResponseErrorHandler {

    private ServiceRegistryLookup srLookup;

    public ServiceBusRestErrorHandler(ServiceRegistryLookup srLookup) {
        this.srLookup = srLookup;
    }

    /**
     * This default implementation throws a {@link HttpClientErrorException} if the response status code
     * is {@link HttpStatus.Series#CLIENT_ERROR}, a {@link HttpServerErrorException}
     * if it is {@link HttpStatus.Series#SERVER_ERROR},
     * and a {@link RestClientException} in other cases.
     *
     * @param response
     */
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = getHttpStatusCode(response);
        if (statusCode == HttpStatus.UNAUTHORIZED) {
            log.debug("Got status {} from service bus, invalidating sas key", statusCode.toString());
            srLookup.invalidateSasKey();
        }
        super.handleError(response);
    }
}
