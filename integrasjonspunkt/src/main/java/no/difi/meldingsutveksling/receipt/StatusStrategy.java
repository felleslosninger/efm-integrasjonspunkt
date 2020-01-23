package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.ServiceIdentifier;

import java.util.Set;

public interface StatusStrategy {
    void checkStatus(Set<Conversation> conversations);

    ServiceIdentifier getServiceIdentifier();
}
