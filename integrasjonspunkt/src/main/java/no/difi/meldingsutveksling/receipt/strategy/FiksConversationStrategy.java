package no.difi.meldingsutveksling.receipt.strategy;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.SvarUtService;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationStrategy;

public class FiksConversationStrategy implements ConversationStrategy {
    private SvarUtService svarUtService;

    public FiksConversationStrategy(SvarUtService svarUtService) {
        this.svarUtService = svarUtService;
    }

    @Override
    public void checkStatus(Conversation conversation) {
        svarUtService.getForsendelseStatus(conversation);
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.FIKS;
    }
}
