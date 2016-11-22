package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.receipt.strategy.DpvConversationStrategy;
import no.difi.meldingsutveksling.receipt.strategy.EduConversationStrategy;
import no.difi.meldingsutveksling.receipt.strategy.NoOperationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConversationStrategyFactory {

    private DpvConversationStrategy dpvConversationStrategy;
    private EduConversationStrategy eduReceiptStrategy;

    @Autowired
    ConversationStrategyFactory(DpvConversationStrategy dpvConversationStrategy,
                                EduConversationStrategy eduReceiptStrategy) {
        this.dpvConversationStrategy = dpvConversationStrategy;
        this.eduReceiptStrategy = eduReceiptStrategy;
    }

    public ConversationStrategy getFactory(Conversation conversation) {
        switch (conversation.getServiceIdentifier()) {
            case DPV:
                return dpvConversationStrategy;
            case EDU:
                return eduReceiptStrategy;
            default:
                return new NoOperationStrategy();
        }
    }
}
