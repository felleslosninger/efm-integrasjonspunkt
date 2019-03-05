package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.NextMoveException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;

@Component
public class NextMoveMessageService {

    public StandardBusinessDocument setDefaults(StandardBusinessDocument sbd) throws NextMoveException {

        List<Scope> scopes = sbd.getStandardBusinessDocumentHeader().getBusinessScope().getScope();
        if (scopes == null || scopes.isEmpty() || scopes.size() > 1) {
            throw new NextMoveException("StandardBusinessDocumentHeader.BusinessScope.Scope must be exactly size one");
        }

        sbd.getConversationScope().ifPresent(s -> {
            if (isNullOrEmpty(s.getInstanceIdentifier())) {
                s.setInstanceIdentifier(createConversationId());
            }
        });
        return sbd;
    }

    private String createConversationId() {
        return UUID.randomUUID().toString();
    }
}
