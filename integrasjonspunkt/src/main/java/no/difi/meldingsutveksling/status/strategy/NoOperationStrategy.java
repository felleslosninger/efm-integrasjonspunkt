package no.difi.meldingsutveksling.status.strategy;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.StatusStrategy;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class NoOperationStrategy implements StatusStrategy {

    @Override
    public void checkStatus(Set<Conversation> conversations) {
        Audit.info("Trying to check a receipt for a message that is not handled by any receipt strategy");
    }

    @NotNull
    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.UNKNOWN;
    }

    @Override
    public boolean isStartPolling(@NotNull MessageStatus status) {
        return false;
    }

    @Override
    public boolean isStopPolling(@NotNull MessageStatus status) {
        return false;
    }
}
