package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

public class P360Client implements NoarkClient {
    private NoarkClientSettings settings;

    public P360Client(NoarkClientSettings settings) {
        this.settings = settings;
    }

    @Override
    public GetCanReceiveMessageResponseType canRecieveMessage(GetCanReceiveMessageRequestType request) {
        return null;
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType request) {
        return null;
    }
}
