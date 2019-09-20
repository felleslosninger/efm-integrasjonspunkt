package no.difi.meldingsutveksling.domain;

import lombok.Data;
import net.logstash.logback.marker.LogstashMarker;

import static no.difi.meldingsutveksling.logging.MarkerFactory.*;

/**
 * Contains information needed to identify a message: who the sender and the receiver is, the original journalpost
 * and the transaction (conversation).
 *
 * To be used with logging and receipts to reduce parameters in methods and dependency to the standard business document.
 */
@Data
public class MessageInfo {

    private final String messageType;
    private final String receiverOrgNumber;
    private final String senderOrgNumber;
    private final String journalPostId;
    private final String conversationId;
    private final String messageId;

    public LogstashMarker createLogstashMarkers() {
        final LogstashMarker mtMarker = messageTypeMarker(getMessageType());
        final LogstashMarker jpMarker = journalPostIdMarker(getJournalPostId());
        final LogstashMarker sMarker = senderMarker(getSenderOrgNumber());
        final LogstashMarker rMarker = receiverMarker(getReceiverOrgNumber());
        final LogstashMarker cidMarker = conversationIdMarker(getConversationId());
        final LogstashMarker miMarker = messageIdMarker(getMessageId());
        return jpMarker.and(sMarker).and(rMarker).and(mtMarker).and(cidMarker).and(miMarker);
    }
}
