package no.difi.meldingsutveksling;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Transport implementation for Altinn message service.
 */
@Component
@RequiredArgsConstructor
public class AltinnTransport {

    private final UUIDGenerator uuidGenerator;
    private final AltinnWsClient client;

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
