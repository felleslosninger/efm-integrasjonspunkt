package no.difi.meldingsutveksling.scheduler;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.IntegrasjonspunktImpl;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.service.QueueService;
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
    private final QueueService queueService;
    private final IntegrasjonspunktImpl integrasjonspunkt;

    private IntegrasjonspunktConfig integrasjonspunktConfig;

    private static final Logger log = LoggerFactory.getLogger(QueueScheduler.class);

    @Autowired
    public QueueScheduler(QueueService queueService, IntegrasjonspunktImpl integrasjonspunkt, IntegrasjonspunktConfig integrasjonspunktConfig) {
        this.queueService = queueService;
        this.integrasjonspunkt = integrasjonspunkt;
        this.integrasjonspunktConfig = integrasjonspunktConfig;
    }

    @Scheduled(cron = FIRE_EVERY_1_MINUTE)
    public void sendMessage() {
        if (integrasjonspunktConfig.isQueueEnabled()) {
            Queue next = queueService.getNext();
            if (next != null) {
                try {
                    PutMessageRequestType request = (PutMessageRequestType) queueService.getMessage(next.getUniqueId());
                    boolean success = integrasjonspunkt.sendMessage(request);
                    applyResultToQueue(next.getUniqueId(), success);
                    sendMessage();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                } catch (IndexOutOfBoundsException e) {
                    applyResultToQueue(next.getUniqueId(), false);
                    log.error("Could not send message.", e.getMessage(), e);
                } catch (MeldingsUtvekslingRuntimeException e) {
                    applyResultToQueue(next.getUniqueId(), false);
                    log.error("Error in message.", e.getMessage(), e);
                }
            }
        }
    }

    private void applyResultToQueue(String uniqueId, boolean result) {
        if (result) {
            queueService.success(uniqueId);
            log.info("Successfully sent message.");
        } else {
            Status status = queueService.fail(uniqueId);

            if (status == Status.RETRY) {
                log.warn("Message failed send. Will try later.");
            } else if (status == Status.ERROR) {
                log.error(Marker.ANY_MARKER, "Message failed. Can not send message to receipient.");
            }
        }
    }
}
