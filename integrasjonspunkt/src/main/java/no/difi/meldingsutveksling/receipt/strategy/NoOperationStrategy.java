package no.difi.meldingsutveksling.receipt.strategy;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.StatusStrategy;

import java.util.Set;

import static no.difi.meldingsutveksling.receipt.ConversationMarker.markerFrom;

public class NoOperationStrategy implements StatusStrategy {

    @Override
    public void checkStatus(Set<Conversation> conversations) {
        Audit.info("Trying to check a receipt for a message that is not handled by any receipt strategy");
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.UNKNOWN;
    }
}
