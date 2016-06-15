package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import org.slf4j.Marker;

class MockRequest implements UploadRequest {

    private final String reference;

    MockRequest() {
        reference = String.valueOf((int) (Math.random() * 100000));
    }

    @Override
        public String getSender() {
        return "910075918";
    }

        @Override
        public String getReceiver() {
        return "910077473";
    }

        @Override
        public String getSenderReference() {
            return reference;
        }

        @Override
        public EduDocument getPayload() {
        return new EduDocument();
    }

    @Override
    public Marker getMarkers() {
        return null;
    }
}
