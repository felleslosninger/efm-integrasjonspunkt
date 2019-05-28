package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.receipt.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
public class TimeToLiveTask {
    private final ConversationService conversationService;
    private final ConversationRepository repo;
    private final MessageStatusFactory messageStatusFactory;
    private final Clock clock;

    @Scheduled(fixedRateString = "${difi.move.nextmove.ttlPollingrate}")
    public void checkStatus() {
        repo.findByExpiryLessThanEqualAndFinished(ZonedDateTime.now(clock), false)
                .forEach(this::setExpired);
    }

    private void setExpired(Conversation c) {
        conversationService.registerStatus(c, messageStatusFactory.getMessageStatus(ReceiptStatus.LEVETID_UTLOPT));
        conversationService.markFinished(c);
    }
}
