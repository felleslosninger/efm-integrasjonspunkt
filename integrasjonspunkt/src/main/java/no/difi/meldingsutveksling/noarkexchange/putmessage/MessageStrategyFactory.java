package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ServiceIdentifier;

public interface MessageStrategyFactory {

    MessageStrategy create(Object payload);

    ServiceIdentifier getServiceIdentifier();
}
