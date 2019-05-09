package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import javax.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;

import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@Slf4j
@RequiredArgsConstructor
public class ConversationResourceUnlocker {

    private final ConversationResourceRepository repo;
    private final Clock clock;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void unlockTimedOutMessages() {
        repo.findByLockTimeoutLessThanEqual(LocalDateTime.now(clock)).forEach(cr -> {
            cr.setLockTimeout(null);
            repo.save(cr);
            log.debug(markerFrom(cr), "Lock for conversation with id={} timed out, releasing", cr.getConversationId());
        });
    }
}
