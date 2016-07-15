package no.difi.meldingsutveksling.noarkexchange.putmessage;

public class PostVirksomhetStrategyFactory implements MessageStrategyFactory {
    @Override
    public PutMessageStrategy create(Object payload) {
        return new PostVirksomhetPutMessageStrategy();
    }
}
