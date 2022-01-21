package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.nextmove.ConversationDirection;

import java.time.OffsetDateTime;

public interface MessageInformable {

    String getConversationId();

    String getMessageId();

    String getSender();

    String getReceiver();

    String getProcessIdentifier();

    String getDocumentIdentifier();

    ConversationDirection getDirection();

    ServiceIdentifier getServiceIdentifier();

    OffsetDateTime getExpiry();
}
