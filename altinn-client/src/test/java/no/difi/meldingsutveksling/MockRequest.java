package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.Request;

public class MockRequest implements Request{
        @Override
        public String getSender() {
        return "123456789";
    }

        @Override
        public String getReceiver() {
        return "987654321";
    }

        @Override
        public String getSenderReference() {
        return "my reference";
    }

        @Override
        public StandardBusinessDocument getPayload() {
        return new StandardBusinessDocument();
    }
}
