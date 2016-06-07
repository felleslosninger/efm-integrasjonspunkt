package no.difi.meldingsutveksling.domain;

/**
 * Contains information needed to identify a message: who the sender and the receiver is, the original journalpost
 * and the transaction (conversation).
 *
 * To be used with logging and receipts to reduce parameters in methods and dependency to the standard business document.
 */
public class MessageInfo {
    String receiverOrgNumber;
    String senderOrgNumber;
    String journalPostId;
    String conversationId;

    public MessageInfo(String receiverOrgNumber, String senderOrgNumber, String journalPostId, String conversationId) {
        this.receiverOrgNumber = receiverOrgNumber;
        this.senderOrgNumber = senderOrgNumber;
        this.journalPostId = journalPostId;
        this.conversationId = conversationId;
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
}
