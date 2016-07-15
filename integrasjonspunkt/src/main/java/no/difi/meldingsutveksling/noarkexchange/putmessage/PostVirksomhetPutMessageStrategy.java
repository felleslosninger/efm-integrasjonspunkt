package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.apache.commons.lang.NotImplementedException;

public class PostVirksomhetPutMessageStrategy implements PutMessageStrategy {
    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType requestType) {
        throw new NotImplementedException("TODO");
    }
}
