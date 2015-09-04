package no.difi.meldingsutveksling.shipping;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

public interface UploadRequest {
    public String getSender();
    public String getReceiver();
    public String getSenderReference();

    StandardBusinessDocument getPayload();
}
