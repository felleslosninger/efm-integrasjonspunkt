package no.difi.meldingsutveksling.status.strategy;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.ConversationService;
import no.difi.meldingsutveksling.api.StatusStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@Order
public class DpfStatusStrategy implements StatusStrategy {

    private SvarUtService svarUtService;
    private ConversationService conversationService;

    public DpfStatusStrategy(SvarUtService svarUtService, ConversationService conversationService) {
        this.svarUtService = svarUtService;
        this.conversationService = conversationService;
    }

    @Override
    public void checkStatus(Conversation conversation) {
        conversationService.registerStatus(conversation, svarUtService.getMessageReceipt(conversation));
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPF;
    }
}
