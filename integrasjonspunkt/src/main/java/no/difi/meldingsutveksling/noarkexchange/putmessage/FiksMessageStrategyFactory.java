package no.difi.meldingsutveksling.noarkexchange.putmessage;

class FiksMessageStrategyFactory implements MessageStrategyFactory{
    @Override
    public MessageStrategy create(Object payload) {
        return null;
    }

    public static FiksMessageStrategyFactory newInstance() {
        return new FiksMessageStrategyFactory();
    }
}
