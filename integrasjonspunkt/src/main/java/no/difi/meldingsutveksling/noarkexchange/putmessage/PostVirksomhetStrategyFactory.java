package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;

public class PostVirksomhetStrategyFactory implements MessageStrategyFactory {

    private final CorrespondenceAgencyConfiguration configuration;
    private final ServiceRegistryLookup serviceRegistryLookup;

    private PostVirksomhetStrategyFactory(CorrespondenceAgencyConfiguration configuration, ServiceRegistryLookup serviceRegistryLookup) {
        this.configuration = configuration;
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    public static PostVirksomhetStrategyFactory newInstance(IntegrasjonspunktProperties properties,
                                                            ServiceRegistryLookup serviceRegistryLookup) {
        return new PostVirksomhetStrategyFactory(
                new CorrespondenceAgencyConfiguration.Builder()
                .withExternalServiceCode(properties.getAltinnPTV().getExternalServiceCode())
                .withExternalServiceEditionCode(properties.getAltinnPTV().getExternalServiceEditionCode())
                .withPassword(properties.getAltinnPTV().getPassword())
                .withSystemUserCode(properties.getAltinnPTV().getUsername())
                .build(),
                serviceRegistryLookup
        );
    }

    @Override
    public MessageStrategy create(Object payload) {
        return new PostVirksomhetMessageStrategy(configuration, serviceRegistryLookup);
    }
}
