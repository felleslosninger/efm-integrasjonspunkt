package no.difi.meldingsutveksling.receipt.strategy;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.SvarUtService;
import no.difi.meldingsutveksling.ks.receipt.DpfReceiptStatus;
import no.difi.meldingsutveksling.receipt.*;

public class FiksStatusStrategy implements StatusStrategy {
    private SvarUtService svarUtService;
    private ConversationRepository conversationRepository;

    public FiksStatusStrategy(SvarUtService svarUtService, ConversationRepository conversationRepository) {
        this.svarUtService = svarUtService;
        this.conversationRepository = conversationRepository;
    }

    @Override
    public void checkStatus(Conversation conversation) {
        final MessageStatus messageStatus = svarUtService.getMessageReceipt(conversation);
        if (messageStatus.getStatus() == DpfReceiptStatus.LEST.toString()) {
            conversation.setPollable(false);
            conversation.setFinished(true);
        }
        conversation.addMessageStatus(messageStatus);
        conversationRepository.save(conversation);
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPF;
    }
}
