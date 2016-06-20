package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import org.slf4j.Marker;

import static no.difi.meldingsutveksling.domain.Organisasjonsnummer.fromIso6523;

public class AltinnWsRequest implements UploadRequest {

    private EduDocument eduDocument;

    public AltinnWsRequest(EduDocument eduDocument) {
        this.eduDocument = eduDocument;
    }

    @Override
        public String getSender() {
            Organisasjonsnummer orgNumberSender = fromIso6523(eduDocument.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue());
            return orgNumberSender.toString();
        }

        @Override
        public String getReceiver() {
            Organisasjonsnummer orgNumberReceiver = fromIso6523(eduDocument.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue());
            return orgNumberReceiver.toString();
        }

        @Override
        public String getSenderReference() {
            return String.valueOf(Math.random() * 3000);
        }

        @Override
        public EduDocument getPayload() {
            return eduDocument;
        }

        /**
         * Delegates creation of logstash markers to EduDocument
         * @return Logstash markers to identify a EduMessage
         */
        @Override
        public Marker getMarkers() {
            return eduDocument.createLogstashMarkers();
        }
}
