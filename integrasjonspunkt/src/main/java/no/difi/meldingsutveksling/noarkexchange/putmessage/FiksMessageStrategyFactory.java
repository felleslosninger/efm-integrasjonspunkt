package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@Component
public class FiksMessageStrategyFactory implements MessageStrategyFactory {

    private final SvarUtService svarUtService;
    private final NoarkClient noarkClient;

    public FiksMessageStrategyFactory(SvarUtService svarUtService,
                                      @Qualifier("localNoark") NoarkClient noarkClient) {
        this.svarUtService = svarUtService;
        this.noarkClient = noarkClient;
    }

    @Override
    public MessageStrategy create(Object payload) {
        return new FiksMessageStrategy(svarUtService, noarkClient);
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPF;
    }

}
