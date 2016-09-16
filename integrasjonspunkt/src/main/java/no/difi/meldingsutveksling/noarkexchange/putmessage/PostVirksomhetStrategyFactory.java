package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.core.env.Environment;

public class PostVirksomhetStrategyFactory implements MessageStrategyFactory {

    private final CorrespondenceAgencyConfiguration configuration;
    private final ServiceRegistryLookup serviceRegistryLookup;

    public PostVirksomhetStrategyFactory(CorrespondenceAgencyConfiguration configuration, ServiceRegistryLookup serviceRegistryLookup) {
        this.configuration = configuration;
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    public static PostVirksomhetStrategyFactory newInstance(Environment environment, ServiceRegistryLookup serviceRegistryLookup) {
        return new PostVirksomhetStrategyFactory(CorrespondenceAgencyConfiguration.configurationFrom(environment), serviceRegistryLookup);
    }

    @Override
    public PutMessageStrategy create(Object payload) {
        return new PostVirksomhetPutMessageStrategy(configuration, serviceRegistryLookup);
    }
}
