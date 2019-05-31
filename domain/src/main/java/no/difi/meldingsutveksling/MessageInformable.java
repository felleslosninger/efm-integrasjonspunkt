package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.nextmove.ConversationDirection;

import java.time.OffsetDateTime;

public interface MessageInformable {

    String getConversationId();

    String getSenderIdentifier();

    String getReceiverIdentifier();

    ConversationDirection getDirection();

    ServiceIdentifier getServiceIdentifier();

    OffsetDateTime getExpiry();
}
