package no.difi.meldingsutveksling.domain;

import net.logstash.logback.marker.LogstashMarker;

import static no.difi.meldingsutveksling.logging.MarkerFactory.*;

/**
 * Contains information needed to identify a message: who the sender and the receiver is, the original journalpost
 * and the transaction (conversation).
 *
 * To be used with logging and receipts to reduce parameters in methods and dependency to the standard business document.
 */
public class MessageInfo {
    private final String messageType;
    private final String receiverOrgNumber;
    private final String senderOrgNumber;
    private final String journalPostId;
    private final String conversationId;

    public MessageInfo(String receiverOrgNumber, String senderOrgNumber, String journalPostId, String conversationId, String messageType) {
        this.receiverOrgNumber = receiverOrgNumber;
        this.senderOrgNumber = senderOrgNumber;
        this.journalPostId = journalPostId;
        this.conversationId = conversationId;
        this.messageType = messageType;
    }

    public String getReceiverOrgNumber() {
        return receiverOrgNumber;
    }

    public String getSenderOrgNumber() {
        return senderOrgNumber;
    }

    public String getJournalPostId() {
        return journalPostId;
    }

    public String getConversationId() {
        return conversationId;
    }

    private String getMessageType() {
        return messageType;
    }

    public LogstashMarker createLogstashMarkers() {
        final LogstashMarker mtMarker = messageTypeMarker(getMessageType());
        final LogstashMarker jpMarker = journalPostIdMarker(getJournalPostId());
        final LogstashMarker sMarker = senderMarker(getSenderOrgNumber());
        final LogstashMarker rMarker = receiverMarker(getReceiverOrgNumber());
        final LogstashMarker cidMarker = conversationIdMarker(getConversationId());
        return jpMarker.and(sMarker).and(rMarker).and(mtMarker).and(cidMarker);
    }
}
