package no.difi.meldingsutveksling.nextmove;

public interface ConversationStrategy {

    void send(ConversationResource conversationResource) throws NextMoveException;
}
