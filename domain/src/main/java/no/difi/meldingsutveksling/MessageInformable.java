package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;

import java.time.OffsetDateTime;

public interface MessageInformable {

    String getConversationId();

    String getMessageId();

    Organisasjonsnummer getSender();

    Organisasjonsnummer getReceiver();

    String getProcessIdentifier();

    ConversationDirection getDirection();

    ServiceIdentifier getServiceIdentifier();

    OffsetDateTime getExpiry();
}
