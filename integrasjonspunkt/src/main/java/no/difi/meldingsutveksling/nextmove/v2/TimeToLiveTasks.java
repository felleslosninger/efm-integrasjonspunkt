package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.status.ConversationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class TimeToLiveTasks {
    private final ConversationRepository repo;
    private final Clock clock;
    private final TimeToLiveExpiredHandler timeToLiveExpiredHandler;

    @Scheduled(fixedRateString = "${difi.move.nextmove.ttlPollingrate}")
    public void checkExpiredMessages() {
        repo.findIdsForExpiredConversations(OffsetDateTime.now(clock))
                .forEach(timeToLiveExpiredHandler::setExpired);
    }
}
