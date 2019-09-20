package no.difi.meldingsutveksling.nextmove;

public interface ConversationStrategy {

    void send(NextMoveOutMessage message) throws NextMoveException;

}
