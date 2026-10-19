package no.difi.meldingsutveksling.api;

import no.difi.meldingsutveksling.nextmove.NextMoveException;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;

public interface ConversationStrategy {

    void send(NextMoveOutMessage message) throws NextMoveException;

}
