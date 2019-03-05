package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;

@Component
public class NextMoveMessageService {

    StandardBusinessDocument setDefaults(StandardBusinessDocument sbd) {
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
