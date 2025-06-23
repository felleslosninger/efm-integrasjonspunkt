package no.difi.meldingsutveksling.altinnv3.DPO.altinn2.shipping;

import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.slf4j.Marker;
import org.springframework.core.io.Resource;

public class UploadRequest {

    private final String senderReference;
    private final StandardBusinessDocument sbd;
    private final Resource asic;

//    public UploadRequest(String senderReference, StandardBusinessDocument sbd) {
//        this(senderReference, sbd, null);
//    }

    public UploadRequest(String senderReference, StandardBusinessDocument sbd, Resource asic) {
        this.senderReference = senderReference;
        this.sbd = sbd;
        this.asic = asic;
    }

    public String getSender() {
        return sbd.getSenderIdentifier().getPrimaryIdentifier();
    }

    public String getReceiver() {
        return sbd.getReceiverIdentifier().getPrimaryIdentifier();
    }

    public String getSenderReference() {
        return senderReference;
    }

    public StandardBusinessDocument getPayload() {
        return sbd;
    }

    public Resource getAsic() {
        return this.asic;
    }

    /**
     * Delegates creation of logstash markers to StandardBusinessDocument
     *
     * @return Logstash markers to identify a EduMessage
     */

    public Marker getMarkers() {
        return SBDUtil.getMessageInfo(sbd).createLogstashMarkers();
    }
}
