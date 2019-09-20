package no.difi.meldingsutveksling.dokumentpakking.service;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.domain.sbdh.CorrelationInformation;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;

import java.time.OffsetDateTime;

@UtilityClass
class ScopeFactory {

    static Scope fromConversationId(String conversationId, String process, OffsetDateTime expectedResponseDateTime) {
        return new Scope()
                .setIdentifier(process)
                .setType(ScopeType.CONVERSATION_ID.toString())
                .setInstanceIdentifier(conversationId)
                .addScopeInformation(new CorrelationInformation()
                        .setExpectedResponseDateTime(expectedResponseDateTime));
    }
}
