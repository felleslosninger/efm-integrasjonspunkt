package no.difi.meldingsutveksling.scheduler;

import no.difi.meldingsutveksling.noarkexchange.IntegrasjonspunktImpl;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class QueueScheduler {
    public static final String FIRE_EVERY_1_MINUTE = "0 0/1 * * * ?";
    private final QueueService queueService;
    private final IntegrasjonspunktImpl integrasjonspunkt;

    @Autowired
    public QueueScheduler(QueueService queueService, IntegrasjonspunktImpl integrasjonspunkt) {
        this.queueService = queueService;
        this.integrasjonspunkt = integrasjonspunkt;
    }

    @Scheduled(cron = FIRE_EVERY_1_MINUTE)
    public void sendMessage() {
        Queue next = queueService.getNext(Status.NEW);
        boolean success = integrasjonspunkt.sendMessage((PutMessageRequestType) queueService.getMessage(next.getUnique()));

        applyResultToQueue(next.getUnique(), success);
    }

    @Scheduled(cron = FIRE_EVERY_1_MINUTE)
    public void retryMessages() {
        Queue next = queueService.getNext(Status.FAILED);
        boolean success = integrasjonspunkt.sendMessage((PutMessageRequestType) queueService.getMessage(next.getUnique()));

        applyResultToQueue(next.getUnique(), success);
    }

    private void applyResultToQueue(String unique, boolean result) {
        if (result) {
            queueService.success(unique);
        }
        else {
            queueService.fail(unique);
        }
    }
}
