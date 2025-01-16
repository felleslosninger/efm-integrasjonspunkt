package no.difi.meldingsutveksling.api;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;

import java.util.Set;

public interface StatusStrategy {

    void checkStatus(Set<Conversation> conversations);
    ServiceIdentifier getServiceIdentifier();
    boolean isStartPolling(MessageStatus status);
    boolean isStopPolling(MessageStatus status);

}
