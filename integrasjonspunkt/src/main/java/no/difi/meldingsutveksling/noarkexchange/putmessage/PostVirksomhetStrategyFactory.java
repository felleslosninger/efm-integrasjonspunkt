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
                .withEndpointURL(properties.getAltinnptv().getEndpointUrl())
                .withExternalServiceCode(properties.getAltinnptv().getExternalServiceCode())
                .withExternalServiceEditionCode(properties.getAltinnptv().getExternalServiceEditionCode())
                .withPassword(properties.getAltinnptv().getPassword())
                .withSystemUserCode(properties.getAltinnptv().getUsername())
                .build()
        );
    }

    @Override
    public MessageStrategy create(Object payload) {
        return new PostVirksomhetMessageStrategy(configuration);
    }
}
