package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.receipt.strategy.NoOperationStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class StatusStrategyFactory {

    private Map<ServiceIdentifier, StatusStrategy> conversationStrategies = new EnumMap<>(ServiceIdentifier.class);

    public StatusStrategyFactory(ObjectProvider<StatusStrategy> statusStrategies) {
        statusStrategies.orderedStream().forEach(s -> {
            conversationStrategies.putIfAbsent(s.getServiceIdentifier(), s);
        });
    }

    StatusStrategy getFactory(Conversation conversation) {
        return conversationStrategies.getOrDefault(conversation.getServiceIdentifier(), new NoOperationStrategy());
    }
}
