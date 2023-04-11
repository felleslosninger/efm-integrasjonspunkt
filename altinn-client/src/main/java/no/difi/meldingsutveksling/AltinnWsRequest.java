package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import org.slf4j.Marker;
import org.springframework.core.io.Resource;

public class AltinnWsRequest implements UploadRequest {

    private final String senderReference;
    private final StandardBusinessDocument sbd;
    private final Resource asic;

    public AltinnWsRequest(String senderReference, StandardBusinessDocument sbd) {
        this(senderReference, sbd, null);
    }

    public AltinnWsRequest(String senderReference, StandardBusinessDocument sbd, Resource asic) {
        this.senderReference = senderReference;
        this.sbd = sbd;
        this.asic = asic;
    }

    @Override
    public String getSender() {
        return sbd.getSenderIdentifier().getPrimaryIdentifier();
    }

    @Override
    public String getReceiver() {
        return sbd.getReceiverIdentifier().getPrimaryIdentifier();
    }

    @Override
    public String getSenderReference() {
        return senderReference;
    }

    @Override
    public StandardBusinessDocument getPayload() {
        return sbd;
    }

    @Override
    public Resource getAsic() {
        return this.asic;
    }

    /**
     * Delegates creation of logstash markers to StandardBusinessDocument
     *
     * @return Logstash markers to identify a EduMessage
     */
    @Override
    public Marker getMarkers() {
        return SBDUtil.getMessageInfo(sbd).createLogstashMarkers();
    }
}
