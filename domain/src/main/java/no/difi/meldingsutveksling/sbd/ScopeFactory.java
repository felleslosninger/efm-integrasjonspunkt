package no.difi.meldingsutveksling.sbd;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.domain.sbdh.CorrelationInformation;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;

import java.time.OffsetDateTime;
import java.util.UUID;

@UtilityClass
public class ScopeFactory {

    public static Scope fromConversationId(String conversationId, String process, OffsetDateTime expectedResponseDateTime) {
        return new Scope()
                .setIdentifier(process)
                .setType(ScopeType.CONVERSATION_ID.toString())
                .setInstanceIdentifier(conversationId)
                .addScopeInformation(new CorrelationInformation()
                        .setExpectedResponseDateTime(expectedResponseDateTime));
    }

    public static Scope fromRef(ScopeType scopeType, String identifier) {
        return new Scope()
                .setType(scopeType.toString())
                .setInstanceIdentifier(identifier);
    }

    public static Scope fromIdentifier(ScopeType scopeType, String identifier) {
        return new Scope()
            .setType(scopeType.toString())
            .setInstanceIdentifier(UUID.randomUUID().toString())
            .setIdentifier(identifier);
    }

}
