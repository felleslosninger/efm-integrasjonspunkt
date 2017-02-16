package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;

public class ScopeFactory {

    public static final String DEFAULT_IDENTIFIER = "urn:no:difi:meldingsutveksling:1.0";

    public static Scope fromJournalPostId(String journalPostId) {
        Scope scope = createDefaultScope();
        scope.setType(ScopeType.JOURNALPOST_ID.name());
        scope.setInstanceIdentifier(journalPostId);
        return scope;
    }

    public static Scope fromConversationId(String conversationId) {
        Scope scope = createDefaultScope();
        scope.setType(ScopeType.CONVERSATION_ID.name());
        scope.setInstanceIdentifier(conversationId);
        return scope;
    }

    public static Scope fromMessagetypeId(String messagetypeId) {
        Scope scope = createDefaultScope();
        scope.setType(ScopeType.MESSAGETYPE_ID.name());
        scope.setInstanceIdentifier(messagetypeId);
        return scope;
    }

    private static Scope createDefaultScope() {
        Scope scope = new Scope();
        scope.setIdentifier(DEFAULT_IDENTIFIER);
        return scope;
    }

}
