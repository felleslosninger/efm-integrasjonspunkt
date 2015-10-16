package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

public interface NoarkClient {
    public boolean canRecieveMessage(String orgnr);

    public PutMessageResponseType sendEduMelding(PutMessageRequestType request);
}
