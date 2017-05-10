package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.ServiceIdentifier;

public interface StatusStrategy {
    void checkStatus(Conversation conversation);

    ServiceIdentifier getServiceIdentifier();
}
