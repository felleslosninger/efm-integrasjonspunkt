package no.difi.meldingsutveksling.noarkexchange.putmessage;

public interface MessageStrategyFactory {

    MessageStrategy create(Object payload);
}
