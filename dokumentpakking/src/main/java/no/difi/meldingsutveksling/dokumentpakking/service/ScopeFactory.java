package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.domain.sbdh.Scope;

public class ScopeFactory {

    public static final String TYPE_JOURNALPOST_ID = "JournalpostId";
    public static final String TYPE_CONVERSATIONID = "ConversationId";
    public static final String DEFAULT_IDENTIFIER = "urn:no:difi:meldingsutveksling:1.0";

    public static Scope fromJournalPostId(String journalPostId) {
        Scope scope = createDefaultScope();
        scope.setType(TYPE_JOURNALPOST_ID);
        scope.setInstanceIdentifier(journalPostId);
        return scope;
    }

    public static final Scope fromConversationId(String conversationId) {
        Scope scope = createDefaultScope();
        scope.setType(TYPE_CONVERSATIONID);
        scope.setInstanceIdentifier(conversationId);
        return scope;
    }

    private static Scope createDefaultScope() {
        Scope scope = new Scope();
        scope.setIdentifier(DEFAULT_IDENTIFIER);
        return scope;
    }

}
