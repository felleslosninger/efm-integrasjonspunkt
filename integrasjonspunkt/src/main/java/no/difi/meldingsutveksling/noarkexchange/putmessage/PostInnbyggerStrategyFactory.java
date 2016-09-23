package no.difi.meldingsutveksling.noarkexchange.putmessage;

public class PostInnbyggerStrategyFactory implements MessageStrategyFactory {

    @Override
    public MessageStrategy create(Object payload) {
        return new PostInnbyggerMessageStrategy(null /*TODO*/);
    }

    public static MessageStrategyFactory newInstance() {
        return new PostInnbyggerStrategyFactory();
    }
}
