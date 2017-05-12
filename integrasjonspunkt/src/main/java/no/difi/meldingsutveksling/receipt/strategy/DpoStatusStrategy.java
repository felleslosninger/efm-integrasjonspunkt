package no.difi.meldingsutveksling.receipt.strategy;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.StatusStrategy;
import org.springframework.stereotype.Component;

@Component
public class DpoStatusStrategy implements StatusStrategy {
    private static final ServiceIdentifier serviceIdentifier = ServiceIdentifier.DPO;

    @Override
    public void checkStatus(Conversation conversation) {
        // No polling needed for DPO.
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return serviceIdentifier;
    }
}
