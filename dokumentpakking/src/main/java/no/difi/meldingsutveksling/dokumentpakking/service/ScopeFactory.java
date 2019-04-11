package no.difi.meldingsutveksling.dokumentpakking.service;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;

@UtilityClass
class ScopeFactory {

    static Scope fromJournalPostId(String journalPostId, String process) {
        return new Scope()
                .setIdentifier(process)
                .setType(ScopeType.JOURNALPOST_ID.toString())
                .setInstanceIdentifier(journalPostId);
    }

    static Scope fromConversationId(String conversationId, String process) {
        return new Scope()
                .setIdentifier(process)
                .setType(ScopeType.CONVERSATION_ID.toString())
                .setInstanceIdentifier(conversationId);
        // TODO add scopeInformation.expectedResponseDateTime
    }
}
