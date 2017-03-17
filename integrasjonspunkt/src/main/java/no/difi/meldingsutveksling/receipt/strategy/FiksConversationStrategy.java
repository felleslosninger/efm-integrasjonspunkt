package no.difi.meldingsutveksling.receipt.strategy;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.ForsendelseStatus;
import no.difi.meldingsutveksling.ks.SvarUtService;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.receipt.ConversationStrategy;

public class FiksConversationStrategy implements ConversationStrategy {
    private SvarUtService svarUtService;
    private ConversationRepository conversationRepository;

    public FiksConversationStrategy(SvarUtService svarUtService, ConversationRepository conversationRepository) {
        this.svarUtService = svarUtService;
        this.conversationRepository = conversationRepository;
    }

    @Override
    public void checkStatus(Conversation conversation) {
        final ForsendelseStatus forsendelseStatus = svarUtService.getMessageReceipt(conversation);
        // TODO: after changing return value of getMessageReceipt to MessageReceipt: add to conversation and save to database
//        MessageReceipt messageReceipt
//        conversation.addMessageReceipt();


    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPF;
    }
}
