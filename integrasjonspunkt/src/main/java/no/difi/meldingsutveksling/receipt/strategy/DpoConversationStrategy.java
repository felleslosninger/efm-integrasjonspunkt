package no.difi.meldingsutveksling.receipt.strategy;

import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationStrategy;
import org.springframework.stereotype.Component;

@Component
public class DpoConversationStrategy implements ConversationStrategy {

    @Override
    public void checkStatus(Conversation conversation) {
        // No polling needed for DPO.
    }
}
