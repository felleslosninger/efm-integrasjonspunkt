package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

/**
 * Abstract
 * @author Glenn Bech
 */
public interface MessageStrategy {
    PutMessageResponseType putMessage(EDUCore request);
}
