package no.difi.meldingsutveksling.transport.altinn;

import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsConfiguration;
import no.difi.meldingsutveksling.AltinnWsRequest;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.transport.Transport;
import org.springframework.context.ApplicationContext;

/**
 * Transport implementation for Altinn message service.
 */
public class AltinnTransport implements Transport {

    private final String endpoint;

    public AltinnTransport(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @param context a configuration object given by the integrasjonspunkt
     * @param eduDocument An eduDocument with a payload consisting of an CMS encrypted ASIC package
     */
    @Override
    public void send(ApplicationContext context, final EduDocument eduDocument) {
        AltinnWsClient client = new AltinnWsClient(AltinnWsConfiguration.fromConfiguration(endpoint, context));
        UploadRequest request1 = new AltinnWsRequest(eduDocument);

        client.send(request1);
    }
}
