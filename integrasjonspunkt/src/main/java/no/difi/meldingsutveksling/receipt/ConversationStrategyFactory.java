package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.receipt.strategy.NoOperationStrategy;

import java.util.EnumMap;
import java.util.Map;

public class ConversationStrategyFactory {
    private Map<ServiceIdentifier, ConversationStrategy> conversationStrategies = new EnumMap<>(ServiceIdentifier.class);

    public void registerStrategy(ConversationStrategy conversationStrategy) {
        conversationStrategies.put(conversationStrategy.getServiceIdentifier(), conversationStrategy);
    }

    ConversationStrategy getFactory(Conversation conversation) {
        return conversationStrategies.getOrDefault(conversation.getServiceIdentifier(), new NoOperationStrategy());
    }
}
