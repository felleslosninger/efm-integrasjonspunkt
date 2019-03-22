package no.difi.meldingsutveksling.transport.altinn;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsRequest;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.transport.Transport;
import org.springframework.context.ApplicationContext;

import java.io.InputStream;

/**
 * Transport implementation for Altinn message service.
 */
@RequiredArgsConstructor
public class AltinnTransport implements Transport {

    private final AltinnWsClient client;
    private final UUIDGenerator uuidGenerator;

    /**
     * @param context a configuration object given by the integrasjonspunkt
     * @param sbd     An sbd with a payload consisting of an CMS encrypted ASIC package
     */
    @Override
    public void send(ApplicationContext context, final StandardBusinessDocument sbd) {
        UploadRequest request = new AltinnWsRequest(uuidGenerator.generate(), sbd);
        client.send(request);
    }

    /**
     * @param sbd             An sbd with a payload consisting of metadata only
     * @param asicInputStream InputStream pointing to the encrypted ASiC package
     */
    @Override
    public void send(ApplicationContext context, StandardBusinessDocument sbd, InputStream asicInputStream) {
        UploadRequest request = new AltinnWsRequest(uuidGenerator.generate(), sbd, asicInputStream);
        client.send(request);
    }
}
