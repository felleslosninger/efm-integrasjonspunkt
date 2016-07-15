package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;

public interface MessageStrategyFactory {

    PutMessageStrategy create(Object payload);
}
