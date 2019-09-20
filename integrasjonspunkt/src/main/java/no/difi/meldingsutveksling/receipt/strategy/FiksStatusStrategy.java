package no.difi.meldingsutveksling.receipt.strategy;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.StatusStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
public class FiksStatusStrategy implements StatusStrategy {

    private SvarUtService svarUtService;
    private ConversationService conversationService;

    public FiksStatusStrategy(SvarUtService svarUtService, ConversationService conversationService) {
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
