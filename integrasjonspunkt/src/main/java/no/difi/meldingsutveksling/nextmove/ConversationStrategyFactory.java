package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Maps;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ConversationStrategyFactory {

    private Map<ServiceIdentifier, ConversationStrategy> strategies;

    @Autowired
    public ConversationStrategyFactory(IntegrasjonspunktProperties props,
                                       DpoConversationStrategy dpoStrat,
                                       DpfConversationStrategy dpfStrat,
                                       DpeConversationStrategy dpeStrat,
                                       DpvConversationStrategy dpvStrat,
                                       DpiConversationStrategy dpiStrat) {
        strategies = Maps.newEnumMap(ServiceIdentifier.class);
        if (props.getFeature().isEnableDPO()) {
            strategies.put(ServiceIdentifier.DPO, dpoStrat);
        }
        if (props.getFeature().isEnableDPF()) {
            strategies.put(ServiceIdentifier.DPF, dpfStrat);
        }
        if (props.getFeature().isEnableDPV()) {
            strategies.put(ServiceIdentifier.DPV, dpvStrat);
        }
        if (props.getFeature().isEnableDPI()) {
            strategies.put(ServiceIdentifier.DPI, dpiStrat);
        }
        if (props.getFeature().isEnableDPE()) {
            strategies.put(ServiceIdentifier.DPE, dpeStrat);
        }
    }

    Optional<ConversationStrategy> getStrategy(ServiceIdentifier serviceIdentifier) {
        return Optional.ofNullable(strategies.get(serviceIdentifier));
    }
}
