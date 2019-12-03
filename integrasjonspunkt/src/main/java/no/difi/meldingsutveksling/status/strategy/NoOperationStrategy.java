package no.difi.meldingsutveksling.status.strategy;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.StatusStrategy;

import static no.difi.meldingsutveksling.status.ConversationMarker.markerFrom;

public class NoOperationStrategy implements StatusStrategy {

    @Override
    public void checkStatus(Conversation conversation) {
        Audit.info("Trying to check a receipt that is not handled by receipt strategy", markerFrom(conversation));
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.UNKNOWN;
    }
}
