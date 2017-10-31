package no.difi.meldingsutveksling.receipt.strategy;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.ks.receipt.DpfReceiptStatus;
import no.difi.meldingsutveksling.receipt.*;

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
        if (DpfReceiptStatus.LEST.toString().equals(messageStatus.getStatus())) {
            conversationService.markFinished(c);
        }
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPF;
    }
}
