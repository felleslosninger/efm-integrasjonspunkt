package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ResponseStatusSender {

    private final ResponseStatusSenderProxy proxy;
    private final IntegrasjonspunktProperties props;
    private final Logger log = LoggerFactory.getLogger(ResponseStatusSender.class);

    public ResponseStatusSender(ResponseStatusSenderProxy proxy, IntegrasjonspunktProperties props) {
        this.proxy = proxy;
        this.props = props;
    }

    /**
     * Sends the status to the sender of the message. When called inside a transaction, the status is
     * sent only after that transaction commits, so a rollback never reports MOTTATT/LEVERT for a message
     * that was not stored/removed. Without an active transaction the status is sent immediately.
     */
    public void queue(StandardBusinessDocument sbd, ServiceIdentifier si, ReceiptStatus status) {
        if (!props.getNextmove().getStatusServices().contains(si)) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(sbd, si, status);
                }
            });
        } else {
            send(sbd, si, status);
        }
    }

    private void send(StandardBusinessDocument sbd, ServiceIdentifier si, ReceiptStatus status) {
        Thread t = new Thread(() -> {
            try {
                proxy.queue(sbd, si, status);
            } catch (Throwable t1) {
                log.error("Error sending status message", t1);
            }
        }, "response-status-sender");
        t.setDaemon(true);
        t.start();
    }

}
