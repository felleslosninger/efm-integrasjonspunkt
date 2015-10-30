package no.difi.meldingsutveksling.shipping;

import no.difi.meldingsutveksling.domain.sbdh.Document;

public interface UploadRequest {
    public String getSender();
    public String getReceiver();
    public String getSenderReference();

    Document getPayload();
}
