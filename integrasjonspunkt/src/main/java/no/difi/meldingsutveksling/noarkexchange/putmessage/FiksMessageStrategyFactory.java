package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ks.SvarUtService;

class FiksMessageStrategyFactory implements MessageStrategyFactory{
    private SvarUtService svarUtService;

    private FiksMessageStrategyFactory(SvarUtService svarUtService) {
        this.svarUtService = svarUtService;
    }

    @Override
    public MessageStrategy create(Object payload) {
        return new FiksMessageStrategy(svarUtService);
    }

    public static FiksMessageStrategyFactory newInstance(SvarUtService svarUtService) {
        return new FiksMessageStrategyFactory(svarUtService);
    }
}
