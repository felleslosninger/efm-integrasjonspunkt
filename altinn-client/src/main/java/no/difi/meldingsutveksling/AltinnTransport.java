package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Transport implementation for Altinn message service.
 */
@Component
public class AltinnTransport {

    private final UUIDGenerator uuidGenerator;
    private final AltinnWsClient client;

    public AltinnTransport(AltinnWsClientFactory altinnWsClientFactory, UUIDGenerator uuidGenerator) {
        this.uuidGenerator = uuidGenerator;
        this.client = altinnWsClientFactory.getAltinnWsClient();
    }

    /**
     * @param sbd     An sbd with a payload consisting of an CMS encrypted ASIC package
     */
    public void send(final StandardBusinessDocument sbd) {
        UploadRequest request = new AltinnWsRequest(uuidGenerator.generate(), sbd);
        client.send(request);
    }

    /**
     * @param sbd             An sbd with a payload consisting of metadata only
     * @param asicInputStream InputStream pointing to the encrypted ASiC package
     */
    public void send(StandardBusinessDocument sbd, InputStream asicInputStream) {
        UploadRequest request = new AltinnWsRequest(uuidGenerator.generate(), sbd, asicInputStream);
        client.send(request);
    }
}
