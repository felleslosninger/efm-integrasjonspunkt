package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

/**
 * Abstract
 * @author Glenn Bech
 */
public interface PutMessageStrategy {
    PutMessageResponseType putMessage(PutMessageRequestType requestType);
}
