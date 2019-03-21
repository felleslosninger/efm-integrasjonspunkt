package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;

public class ScopeFactory {

    public static final String DEFAULT_IDENTIFIER = "urn:no:difi:meldingsutveksling:1.0";

    public static Scope fromJournalPostId(String journalPostId) {
        Scope scope = createDefaultScope();
        scope.setType(ScopeType.JOURNALPOST_ID.toString());
        scope.setInstanceIdentifier(journalPostId);
        return scope;
    }

    public static Scope fromConversationId(String conversationId) {
        Scope scope = createDefaultScope();
        scope.setType(ScopeType.CONVERSATION_ID.toString());
        scope.setInstanceIdentifier(conversationId);
        return scope;
    }

    public static Scope fromConversationId(String conversationId, ServiceIdentifier serviceIdentifier) {
        Scope scope = new Scope();
        scope.setIdentifier(serviceIdentifier.getStandard());
        scope.setType(ScopeType.CONVERSATION_ID.toString());
        scope.setInstanceIdentifier(conversationId);
        // TODO add scopeInformation.expectedResponseDateTime
        return scope;
    }

    private static Scope createDefaultScope() {
        Scope scope = new Scope();
        scope.setIdentifier(DEFAULT_IDENTIFIER);
        return scope;
    }

}
