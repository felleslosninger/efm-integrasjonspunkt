package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

/**
 * Generic send message interface
 *
 * @author Glenn Bech
 */

public interface SendMessageTemplate {
    PutMessageResponseType sendMessage(PutMessageRequestType message);
}
