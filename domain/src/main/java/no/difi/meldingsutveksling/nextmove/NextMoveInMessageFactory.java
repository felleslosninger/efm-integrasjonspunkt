package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NextMoveInMessageFactory {

    private final SBDService sbdService;

    public NextMoveInMessage of(StandardBusinessDocument sbd, ServiceIdentifier serviceIdentifier) {
        return new NextMoveInMessage(
                SBDUtil.getConversationId(sbd),
                SBDUtil.getMessageId(sbd),
                SBDUtil.getProcess(sbd),
                sbdService.getReceiverIdentifier(sbd),
                sbdService.getSenderIdentifier(sbd),
                serviceIdentifier,
                sbd);
    }
}
