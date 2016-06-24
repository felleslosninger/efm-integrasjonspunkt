package no.difi.meldingsutveksling.transport.altinn;

import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsConfiguration;
import no.difi.meldingsutveksling.AltinnWsRequest;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.elma.ELMALookup;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.vefa.peppol.common.model.Endpoint;
import no.difi.vefa.peppol.lookup.api.LookupException;
import org.springframework.core.env.Environment;


/**
 * Transport implementation for Altinn message service.
 */
public class AltinnTransport implements Transport {

    private final String endpoint;

    public AltinnTransport(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @param environment a configuration object given by the integrasjonspunkt
     * @param eduDocument      An eduDocument with a payload consisting of an CMS encrypted ASIC package
     */
    @Override
    public void send(Environment environment, final EduDocument eduDocument) {
        AltinnWsClient client = new AltinnWsClient(AltinnWsConfiguration.fromConfiguration(endpoint, environment));
        UploadRequest request1 = new AltinnWsRequest(eduDocument);

        client.send(request1);
    }
}
