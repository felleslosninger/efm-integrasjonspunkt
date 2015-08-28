package no.difi.meldingsutveksling.shipping;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

public interface Request {
    public String getSender();
    public String getReceiver();
    public String getSenderReference();

    StandardBusinessDocument getPayload();
}
