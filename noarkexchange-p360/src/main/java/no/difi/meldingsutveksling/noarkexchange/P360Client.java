package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

public class P360Client implements NoarkClient {
    private NoarkClientSettings settings;

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType request) {
        return null;
    }

    @Override
    public void setSettings(NoarkClientSettings noarkClientSettings) {
        this.settings = noarkClientSettings;
    }
}
