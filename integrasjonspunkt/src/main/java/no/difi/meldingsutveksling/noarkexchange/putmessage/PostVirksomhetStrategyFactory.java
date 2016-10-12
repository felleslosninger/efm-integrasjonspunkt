package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;

public class PostVirksomhetStrategyFactory implements MessageStrategyFactory {

    private final CorrespondenceAgencyConfiguration configuration;

    public PostVirksomhetStrategyFactory(CorrespondenceAgencyConfiguration configuration) {
        this.configuration = configuration;
    }

    public static PostVirksomhetStrategyFactory newInstance(IntegrasjonspunktProperties properties) {
        return new PostVirksomhetStrategyFactory(
                new CorrespondenceAgencyConfiguration.Builder()
                .withEndpointURL(properties.getAltinnPTV().getEndpointUrl())
                .withExternalServiceCode(properties.getAltinnPTV().getExternalServiceCode())
                .withExternalServiceEditionCode(properties.getAltinnPTV().getExternalServiceEditionCode())
                .withPassword(properties.getAltinnPTV().getPassword())
                .withSystemUserCode(properties.getAltinnPTV().getUsername())
                .build()
        );
    }

    @Override
    public MessageStrategy create(Object payload) {
        return new PostVirksomhetMessageStrategy(configuration);
    }
}
