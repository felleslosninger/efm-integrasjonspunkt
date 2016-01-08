package no.difi.meldingsutveksling.scheduler;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.MessageMarkerFactory;
import no.difi.meldingsutveksling.noarkexchange.IntegrasjonspunktImpl;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.queue.domain.QueueElement;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.exception.QueueException;
import no.difi.meldingsutveksling.queue.service.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@EnableScheduling
public class QueueScheduler {
    public static final String FIRE_EVERY_1_MINUTE = "0 0/1 * * * ?";
    private final Queue queue;
    private final IntegrasjonspunktImpl integrasjonspunkt;

    private IntegrasjonspunktConfiguration integrasjonspunktConfig;

    private static final Logger log = LoggerFactory.getLogger(QueueScheduler.class);

    @Autowired
    public QueueScheduler(Queue queue, IntegrasjonspunktImpl integrasjonspunkt,
                          IntegrasjonspunktConfiguration integrasjonspunktConfig) {
        this.queue = queue;
        this.integrasjonspunkt = integrasjonspunkt;
        this.integrasjonspunktConfig = integrasjonspunktConfig;
    }

    @Scheduled(cron = FIRE_EVERY_1_MINUTE)
    public void sendMessage() {
        if (integrasjonspunktConfig.isQueueEnabled()) {
            QueueElement next = queue.getNext();
            if (next != null) {
                try {
                    PutMessageRequestType request = (PutMessageRequestType) queue.getMessage(next.getUniqueId());
                    boolean success = integrasjonspunkt.sendMessage(request);
                    final Status status = applyResultToQueue(next.getUniqueId(), success);
                    logStatus(status, request);
                    sendMessage();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                } catch (IndexOutOfBoundsException e) {
                    applyResultToQueue(next.getUniqueId(), false);
                    log.error("Could not send message.", e.getMessage(), e);
                } catch (MeldingsUtvekslingRuntimeException e) {
                    applyResultToQueue(next.getUniqueId(), false);
                    log.error("Error in message.", e.getMessage(), e);
                } catch (QueueException e) {
                    applyResultToQueue(next.getUniqueId(), false);
                    log.error("Internal error in queue.", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Method to audit log and technical log message delivery based on queue status
     * @param status either RETRY or FAILED will log
     * @param request
     */
    private void logStatus(Status status, PutMessageRequestType request) {
        final PutMessageRequestWrapper message = new PutMessageRequestWrapper(request);
        if (status == Status.RETRY) {
            Audit.info("Failed to send message. It is put on retry queue", message);
            log.warn(MessageMarkerFactory.markerFrom(message), "Message failed send. Will try later.");
        } else if (status == Status.ERROR) {
            Audit.error("Message could not be delivered after several attempts", message);
            log.error(MessageMarkerFactory.markerFrom(message), "Message failed. Can not send message to receipient.");
        }

    }

    /**
     * Updates the queue element based on the result of sending the message
     * @param uniqueId the id to the queue element
     * @param result true if the message was successfully sent, false otherwise
     * @return the Status of the queue element that was updated
     */
    private Status applyResultToQueue(String uniqueId, boolean result) {
        final Status status;
        if (result) {
            status = queue.success(uniqueId);
        } else {
            status = queue.fail(uniqueId);
        }
        return status;
    }
}
