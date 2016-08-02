package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;

public class PostVirksomhetStrategyFactory implements MessageStrategyFactory {

    private final IntegrasjonspunktConfiguration configuration;

    public PostVirksomhetStrategyFactory(IntegrasjonspunktConfiguration configuration) {
        this.configuration = configuration;
    }

    public static PostVirksomhetStrategyFactory newInstance(MessageSender messageSender) {
        return new PostVirksomhetStrategyFactory(messageSender.getConfiguration());
    }

    @Override
    public PutMessageStrategy create(Object payload) {
        CorrespondenceAgencyConfiguration correspondenceAgencyConfig = configuration.getPostTilVirksomhetConfig();

        return new PostVirksomhetPutMessageStrategy(correspondenceAgencyConfig);
    }
}
