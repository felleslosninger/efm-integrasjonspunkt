package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;

import java.time.OffsetDateTime;

public interface MessageInformable {

    String getConversationId();

    String getMessageId();

    PartnerIdentifier getSender();

    PartnerIdentifier getReceiver();

    String getProcessIdentifier();

    String getDocumentIdentifier();

    ConversationDirection getDirection();

    ServiceIdentifier getServiceIdentifier();

    OffsetDateTime getExpiry();
}
