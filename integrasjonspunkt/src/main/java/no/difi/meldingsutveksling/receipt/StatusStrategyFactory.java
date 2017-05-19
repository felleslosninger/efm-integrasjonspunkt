package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.receipt.strategy.NoOperationStrategy;

import java.util.EnumMap;
import java.util.Map;

public class StatusStrategyFactory {
    private Map<ServiceIdentifier, StatusStrategy> conversationStrategies = new EnumMap<>(ServiceIdentifier.class);

    public void registerStrategy(StatusStrategy conversationStrategy) {
        conversationStrategies.put(conversationStrategy.getServiceIdentifier(), conversationStrategy);
    }

    StatusStrategy getFactory(Conversation conversation) {
        return conversationStrategies.getOrDefault(conversation.getServiceIdentifier(), new NoOperationStrategy());
    }
}
