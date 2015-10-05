package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

/**
 * Abstract
 * @author Glenn Bech
 */
interface PutMessageStrategy {
    PutMessageResponseType putMessage(PutMessageRequestType requestType);
}
