package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import org.slf4j.Marker;

import java.io.InputStream;

import static no.difi.meldingsutveksling.domain.Organisasjonsnummer.fromIso6523;

public class AltinnWsRequest implements UploadRequest {

    private final String senderReference;
    private final StandardBusinessDocument sbd;
    private InputStream asicInputStream;

    public AltinnWsRequest(String senderReference, StandardBusinessDocument sbd) {
        this.senderReference = senderReference;
        this.sbd = sbd;
    }

    public AltinnWsRequest(String senderReference, StandardBusinessDocument sbd, InputStream is) {
        this.senderReference = senderReference;
        this.sbd = sbd;
        this.asicInputStream = is;
    }

    @Override
    public String getSender() {
        Organisasjonsnummer orgNumberSender = fromIso6523(sbd.getStandardBusinessDocumentHeader().getSender().iterator().next().getIdentifier().getValue());
        return orgNumberSender.toString();
    }

    @Override
    public String getReceiver() {
        Organisasjonsnummer orgNumberReceiver = fromIso6523(sbd.getStandardBusinessDocumentHeader().getReceiver().iterator().next().getIdentifier().getValue());
        return orgNumberReceiver.toString();
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
    public InputStream getAsicInputStream() {
        return this.asicInputStream;
    }

    /**
     * Delegates creation of logstash markers to StandardBusinessDocument
     *
     * @return Logstash markers to identify a EduMessage
     */
    @Override
    public Marker getMarkers() {
        return sbd.createLogstashMarkers();
    }
}
