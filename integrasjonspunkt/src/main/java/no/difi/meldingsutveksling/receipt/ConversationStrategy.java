package no.difi.meldingsutveksling.receipt;

public interface ConversationStrategy {
    void checkStatus(Conversation conversation);
}
