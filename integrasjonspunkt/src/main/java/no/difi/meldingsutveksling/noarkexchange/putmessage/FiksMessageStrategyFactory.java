package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;

public class FiksMessageStrategyFactory implements MessageStrategyFactory{
    private SvarUtService svarUtService;
    private NoarkClient noarkClient;

    private FiksMessageStrategyFactory(SvarUtService svarUtService, NoarkClient noarkClient) {
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

    public static FiksMessageStrategyFactory newInstance(SvarUtService svarUtService, NoarkClient noarkClient) {
        return new FiksMessageStrategyFactory(svarUtService, noarkClient);
    }
}
