package no.difi.meldingsutveksling.receipt.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.svarut.ForsendelseIdService;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.StatusStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
public class FiksStatusStrategy implements StatusStrategy {

    private final SvarUtService svarUtService;
    private final ConversationService conversationService;
    private final ForsendelseIdService forsendelseIdService;

    @Override
    @Transactional
    public void checkStatus(Set<Conversation> conversations) {
        svarUtService.getMessageStatuses(conversations).forEach((c,s) -> {
            conversationService.registerStatus(c, s);
            if (!c.isPollable()) {
                forsendelseIdService.delete(c.getMessageId());
            }
        });
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPF;
    }
}
