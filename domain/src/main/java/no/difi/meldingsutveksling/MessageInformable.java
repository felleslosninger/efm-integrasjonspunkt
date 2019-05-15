package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.nextmove.ConversationDirection;

public interface MessageInformable {

    String getConversationId();

    String getSenderIdentifier();

    String getReceiverIdentifier();

    ConversationDirection getDirection();

    ServiceIdentifier getServiceIdentifier();
}
