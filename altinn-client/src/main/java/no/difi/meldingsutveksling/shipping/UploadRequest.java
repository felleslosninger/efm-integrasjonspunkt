package no.difi.meldingsutveksling.shipping;

import no.difi.meldingsutveksling.domain.sbdh.EduDocument;

public interface UploadRequest {
    String getSender();
    String getReceiver();
    String getSenderReference();

    EduDocument getPayload();
}
