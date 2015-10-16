package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

public interface NoarkClient {
    public GetCanReceiveMessageResponseType canRecieveMessage(GetCanReceiveMessageRequestType request);

    public PutMessageResponseType putMessage(PutMessageRequestType request);
}
