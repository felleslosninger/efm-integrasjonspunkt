package no.difi.meldingsutveksling.domain.sbdh;

public enum ScopeType {
    JOURNALPOST_ID("JournalpostId"),
    CONVERSATION_ID("ConversationId"),
    SENDER_REF("SenderRef"),
    RECEIVER_REF("ReceiverRef");

    private String fullname;

    ScopeType(String fullname) {
        this.fullname = fullname;
    }

    public String getFullname() {
        return fullname;
    }

    @Override
    public String toString() {
        return this.fullname;
    }
}
