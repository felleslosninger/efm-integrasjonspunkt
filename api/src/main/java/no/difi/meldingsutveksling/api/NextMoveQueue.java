package no.difi.meldingsutveksling.api;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.core.io.Resource;

public interface NextMoveQueue {

    /**
     * Enqueue an incoming nextmove message. [asic] is closed.
     */
    void enqueueIncomingMessage(StandardBusinessDocument sbd,ServiceIdentifier serviceIdentifier, Resource asic);

    /**
     * Enqueue an incoming message.
     */
    void enqueueIncomingMessage(StandardBusinessDocument sbd, ServiceIdentifier serviceIdentifier);

}