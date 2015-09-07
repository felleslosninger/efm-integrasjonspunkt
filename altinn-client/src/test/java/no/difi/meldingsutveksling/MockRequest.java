package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;

public class MockRequest implements UploadRequest {

    private final String reference;

    public MockRequest() {
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
        public StandardBusinessDocument getPayload() {
        return new StandardBusinessDocument();
    }
}
