package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Maps;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ConversationStrategyFactory {

    private Map<ServiceIdentifier, ConversationStrategy> strategies;

    public ConversationStrategyFactory(IntegrasjonspunktProperties props,
                                       ObjectProvider<DpoConversationStrategy> dpoStrat,
                                       ObjectProvider<DpfConversationStrategy> dpfStrat,
                                       ObjectProvider<DpeConversationStrategy> dpeStrat,
                                       ObjectProvider<DpvConversationStrategy> dpvStrat,
                                       ObjectProvider<DpiConversationStrategy> dpiStrat) {
        strategies = Maps.newEnumMap(ServiceIdentifier.class);
        if (props.getFeature().isEnableDPO()) {
            strategies.put(ServiceIdentifier.DPO, dpoStrat.getIfAvailable());
        }
        if (props.getFeature().isEnableDPF()) {
            strategies.put(ServiceIdentifier.DPF, dpfStrat.getIfAvailable());
        }
        if (props.getFeature().isEnableDPV()) {
            strategies.put(ServiceIdentifier.DPV, dpvStrat.getIfAvailable());
        }
        if (props.getFeature().isEnableDPI()) {
            strategies.put(ServiceIdentifier.DPI, dpiStrat.getIfAvailable());
        }
        if (props.getFeature().isEnableDPE()) {
            strategies.put(ServiceIdentifier.DPE, dpeStrat.getIfAvailable());
        }
    }

    public Optional<ConversationStrategy> getStrategy(ServiceIdentifier serviceIdentifier) {
        return Optional.ofNullable(strategies.get(serviceIdentifier));
    }
}
