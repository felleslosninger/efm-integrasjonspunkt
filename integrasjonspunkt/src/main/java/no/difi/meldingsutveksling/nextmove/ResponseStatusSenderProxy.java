package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class ResponseStatusSenderProxy {

    private final InternalQueue internalQueue;
    private final SBDFactory sbdFactory;

    public ResponseStatusSenderProxy(@Lazy InternalQueue internalQueue, SBDFactory sbdFactory) {
        this.internalQueue = internalQueue;
        this.sbdFactory = sbdFactory;
    }

    @Retryable(maxAttempts = 10, backoff = @Backoff(delay = 5000, multiplier = 2.0, maxDelay = 1000 * 60 * 10L))
    public void queue(StandardBusinessDocument sbd, ServiceIdentifier si, ReceiptStatus status) {
        StandardBusinessDocument statusDoc = sbdFactory.createStatusFrom(sbd, status);
        if (statusDoc != null) {
            NextMoveOutMessage out = NextMoveOutMessage.of(statusDoc, si);
            if (out != null) {
                internalQueue.enqueueNextMove(out);
            }
        }
    }

}
