package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

public interface NoarkClient {
    boolean canRecieveMessage(String orgnr);

    PutMessageResponseType sendEduMelding(PutMessageRequestType request);
}
