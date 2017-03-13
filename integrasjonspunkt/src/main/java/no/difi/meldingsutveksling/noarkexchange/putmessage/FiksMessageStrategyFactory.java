package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.SvarUtService;

public class FiksMessageStrategyFactory implements MessageStrategyFactory{
    private SvarUtService svarUtService;

    private FiksMessageStrategyFactory(SvarUtService svarUtService) {
        this.svarUtService = svarUtService;
    }

    @Override
    public MessageStrategy create(Object payload) {
        return new FiksMessageStrategy(svarUtService);
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.FIKS;
    }

    public static FiksMessageStrategyFactory newInstance(SvarUtService svarUtService) {
        return new FiksMessageStrategyFactory(svarUtService);
    }
}
