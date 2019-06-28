package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class ServiceBusRestErrorHandler extends DefaultResponseErrorHandler {

    private final ServiceRegistryLookup srLookup;

    @Override
    protected void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
        if (statusCode == HttpStatus.UNAUTHORIZED) {
            log.debug("Got status {} from service bus, invalidating sas key", statusCode.toString());
            srLookup.invalidateSasKey();
        }
        super.handleError(response);
    }
}
