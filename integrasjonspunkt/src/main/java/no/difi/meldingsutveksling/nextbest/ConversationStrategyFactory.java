package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ConversationStrategyFactory {

    private IntegrasjonspunktProperties props;

    private Map<ServiceIdentifier, ConversationStrategy> strategies;

    @Autowired
    public ConversationStrategyFactory(IntegrasjonspunktProperties props,
                                       DpoConversationStrategy dpoStrat,
                                       DpeConversationStrategy dpeStrat,
                                       DpvConversationStrategy dpvStrat) {
        strategies = Maps.newEnumMap(ServiceIdentifier.class);
        if (props.getFeature().isEnableDPO()) {
            strategies.put(ServiceIdentifier.DPO, dpoStrat);
        }
        if (props.getFeature().isEnableDPV()) {
            strategies.put(ServiceIdentifier.DPV, dpvStrat);
        }
        if (props.getFeature().isEnableDPE()) {
            strategies.put(ServiceIdentifier.DPE_INNSYN, dpeStrat);
            strategies.put(ServiceIdentifier.DPE_DATA, dpeStrat);
        }
    }

    public List<ServiceIdentifier> getEnabledServices() {
        return Lists.newArrayList(strategies.keySet());
    }

    public Optional<ConversationStrategy> getStrategy(ConversationResource conversationResource) {
        return Optional.ofNullable(strategies.get(conversationResource.getServiceIdentifier()));
    }
}
