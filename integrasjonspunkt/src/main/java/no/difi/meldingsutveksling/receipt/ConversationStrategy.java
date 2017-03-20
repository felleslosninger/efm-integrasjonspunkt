package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.ServiceIdentifier;

public interface ConversationStrategy {
    void checkStatus(Conversation conversation);

    ServiceIdentifier getServiceIdentifier();
}
