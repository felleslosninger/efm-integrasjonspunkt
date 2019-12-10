package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.*;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static no.difi.meldingsutveksling.ServiceIdentifier.*;

@Component
public class ConversationStrategyFactory {

    private Map<ServiceIdentifier, ConversationStrategy> strategies;

    public ConversationStrategyFactory(IntegrasjonspunktProperties props,
                                       ObjectProvider<DpoConversationStrategy> dpoStrat,
                                       ObjectProvider<DpfConversationStrategy> dpfStrat,
                                       ObjectProvider<DpeConversationStrategy> dpeStrat,
                                       ObjectProvider<DpvConversationStrategy> dpvStrat,
                                       ObjectProvider<DpiConversationStrategy> dpiStrat) {
        strategies = new EnumMap<>(ServiceIdentifier.class);
        if (props.getFeature().isEnableDPO()) {
            dpoStrat.orderedStream().findFirst().ifPresent(s -> strategies.put(DPO, s));
        }
        if (props.getFeature().isEnableDPF()) {
            dpfStrat.orderedStream().findFirst().ifPresent(s -> strategies.put(DPF, s));
        }
        if (props.getFeature().isEnableDPV()) {
            dpvStrat.orderedStream().findFirst().ifPresent(s -> strategies.put(DPV, s));
        }
        if (props.getFeature().isEnableDPI()) {
            dpiStrat.orderedStream().findFirst().ifPresent(s -> strategies.put(DPI, s));
        }
        if (props.getFeature().isEnableDPE()) {
            dpeStrat.orderedStream().findFirst().ifPresent(s -> strategies.put(DPE, s));
        }
    }

    public Optional<ConversationStrategy> getStrategy(ServiceIdentifier serviceIdentifier) {
        return Optional.ofNullable(strategies.get(serviceIdentifier));
    }
}
