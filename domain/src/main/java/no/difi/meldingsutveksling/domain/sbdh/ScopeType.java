package no.difi.meldingsutveksling.domain.sbdh;

public enum ScopeType {
    JOURNALPOST_ID("JournalpostId"),
    CONVERSATION_ID("ConversationId");

    private String fullname;

    ScopeType(String fullname) {
        this.fullname = fullname;
    }

    @Override
    public String toString() {
        return this.fullname;
    }
}
