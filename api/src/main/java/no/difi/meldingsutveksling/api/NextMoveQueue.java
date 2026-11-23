package no.difi.meldingsutveksling.api;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.core.io.Resource;

public interface NextMoveQueue {

    /**
     * Enqueue an incoming nextmove message. Unless enqueueing fails, [asic] is read to the end, also when the message
     * is not put on the queue. It is not closed: the caller closes it.
     */
    EnqueueResult enqueueIncomingMessage(StandardBusinessDocument sbd,ServiceIdentifier serviceIdentifier, Resource asic);

    /**
     * Enqueue an incoming message.
     */
    EnqueueResult enqueueIncomingMessage(StandardBusinessDocument sbd, ServiceIdentifier serviceIdentifier);

    /**
     * What became of an incoming message.
     */
    enum EnqueueResult {
        /**
         * The message is put on the local queue.
         */
        ENQUEUED,
        /**
         * A message with the same id is already on the local queue, so the message is discarded.
         */
        DUPLICATE,
        /**
         * The message had expired. Only its expiry is registered, and it is not put on the local queue.
         */
        EXPIRED,
        /**
         * The message is a status, which is registered on its conversation.
         */
        STATUS
    }
}
