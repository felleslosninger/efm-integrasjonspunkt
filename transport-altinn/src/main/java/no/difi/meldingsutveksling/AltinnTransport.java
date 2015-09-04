package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.transport.Transport;

public class AltinnTransport implements Transport {
    public static void main(String[] args) {
        new AltinnTransport().send(new StandardBusinessDocument());
    }

    // create zip from standard business document
    // upload zip file (need to merge in Ftp branch)
    // get receipt <-- ok
    @Override
    public void send(StandardBusinessDocument document) {
        UploadRequest request1 = new UploadRequest() {

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
        };
    }
}
