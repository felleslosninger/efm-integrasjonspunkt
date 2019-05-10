package no.difi.meldingsutveksling.receipt.strategy;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.receipt.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.receipt.ConversationMarker.markerFrom;

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
        final MessageStatus messageStatus = svarUtService.getMessageReceipt(conversation);
        Conversation c = conversationService.registerStatus(conversation, messageStatus);
        if (ReceiptStatus.LEST.toString().equals(messageStatus.getStatus())) {
            conversationService.markFinished(c);
        }
        if (ReceiptStatus.FEIL.toString().equals(messageStatus.getStatus())) {
            conversationService.markFinished(c);
            log.error(markerFrom(c), "DPF conversation {} finished with status {}", c.getConversationId(), messageStatus.getStatus());
        }
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPF;
    }
}
