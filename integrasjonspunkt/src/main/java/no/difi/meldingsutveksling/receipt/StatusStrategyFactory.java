package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.StatusStrategy;
import no.difi.meldingsutveksling.status.strategy.NoOperationStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class StatusStrategyFactory {

    private final Map<ServiceIdentifier, StatusStrategy> statusStrategies = new HashMap<>();

    public StatusStrategyFactory(ObjectProvider<StatusStrategy> strategies) {
        strategies.orderedStream().forEach(s -> statusStrategies.put(s.getServiceIdentifier(), s));
    }

    public StatusStrategy getStrategy(ServiceIdentifier si) {
        return statusStrategies.getOrDefault(si, new NoOperationStrategy());
    }

}
