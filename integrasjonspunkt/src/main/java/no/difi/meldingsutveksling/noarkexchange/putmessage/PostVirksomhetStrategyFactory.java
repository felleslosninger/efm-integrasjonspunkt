package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.core.env.Environment;

public class PostVirksomhetStrategyFactory implements MessageStrategyFactory {

    private final CorrespondenceAgencyConfiguration configuration;

    public PostVirksomhetStrategyFactory(CorrespondenceAgencyConfiguration configuration) {
        this.configuration = configuration;
    }

    public static PostVirksomhetStrategyFactory newInstance(Environment environment) {
        return new PostVirksomhetStrategyFactory(CorrespondenceAgencyConfiguration.configurationFrom(environment));
    }

    @Override
    public MessageStrategy create(Object payload) {
        return new PostVirksomhetMessageStrategy(configuration);
    }
}
