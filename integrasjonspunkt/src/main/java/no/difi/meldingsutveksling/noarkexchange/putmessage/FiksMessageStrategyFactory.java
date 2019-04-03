package no.difi.meldingsutveksling.noarkexchange.putmessage;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;

@RequiredArgsConstructor
public class FiksMessageStrategyFactory implements MessageStrategyFactory {
    private final SvarUtService svarUtService;
    private final NoarkClient noarkClient;

    @Override
    public MessageStrategy create(Object payload) {
        return new FiksMessageStrategy(svarUtService, noarkClient);
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPF;
    }

    public static FiksMessageStrategyFactory newInstance(SvarUtService svarUtService, NoarkClient noarkClient) {
        return new FiksMessageStrategyFactory(svarUtService, noarkClient);
    }
}
