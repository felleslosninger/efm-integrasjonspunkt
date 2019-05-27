package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.receipt.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TimeToLiveTask {
    private final ConversationService conversationService;
    private final ConversationRepository repo;
    private final MessageStatusFactory messageStatusFactory;

    @Scheduled(fixedRateString = "${difi.move.nextmove.ttlPollingrate}")
    public void checkStatus() {
        List<Conversation> conversations = repo.findByExpiryLessThanEqualAndFinished(ZonedDateTime.now(), false);
        for (Conversation c : conversations) {
            conversationService.registerStatus(c, messageStatusFactory.getMessageStatus(ReceiptStatus.LEVETID_UTLOPT));
            conversationService.markFinished(c);
        }
    }
}
