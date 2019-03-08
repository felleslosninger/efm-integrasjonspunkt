package no.difi.meldingsutveksling.transport.altinn;

import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsConfiguration;
import no.difi.meldingsutveksling.AltinnWsRequest;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.transport.Transport;
import org.springframework.context.ApplicationContext;

import java.io.InputStream;

/**
 * Transport implementation for Altinn message service.
 */
public class AltinnTransport implements Transport {

    private final ServiceRecord serviceRecord;

    public AltinnTransport(ServiceRecord serviceRecord) {
        this.serviceRecord = serviceRecord;
    }

    /**
     * @param context a configuration object given by the integrasjonspunkt
     * @param sbd An sbd with a payload consisting of an CMS encrypted ASIC package
     */
    @Override
    public void send(ApplicationContext context, final StandardBusinessDocument sbd) {
        AltinnWsClient client = new AltinnWsClient(AltinnWsConfiguration.fromConfiguration(serviceRecord, context), context);
        UploadRequest request = new AltinnWsRequest(sbd);

        client.send(request);
    }

    /**
     * @param sbd                  An sbd with a payload consisting of metadata only
     * @param asicInputStream      InputStream pointing to the encrypted ASiC package
     */
    @Override
    public void send(ApplicationContext context, StandardBusinessDocument sbd, InputStream asicInputStream) {
        AltinnWsClient client = new AltinnWsClient(AltinnWsConfiguration.fromConfiguration(serviceRecord, context), context);
        UploadRequest request = new AltinnWsRequest(sbd, asicInputStream);

        client.send(request);
    }

}
