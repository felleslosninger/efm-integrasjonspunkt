package no.difi.meldingsutveksling.receipt.strategy;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.SvarUtService;
import no.difi.meldingsutveksling.receipt.*;

public class FiksConversationStrategy implements ConversationStrategy {
    private SvarUtService svarUtService;
    private ConversationRepository conversationRepository;

    public FiksConversationStrategy(SvarUtService svarUtService, ConversationRepository conversationRepository) {
        this.svarUtService = svarUtService;
        this.conversationRepository = conversationRepository;
    }

    @Override
    public void checkStatus(Conversation conversation) {
        final MessageReceipt messageReceipt = svarUtService.getMessageReceipt(conversation);
        if (messageReceipt.getStatus() == ReceiptStatus.READ) {
            conversation.setPollable(false);
        }
        conversation.addMessageReceipt(messageReceipt);
        conversationRepository.save(conversation);
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPF;
    }
}
