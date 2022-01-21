package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.logging.NextMoveMessageMarkers;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Slf4j
@RequiredArgsConstructor
public class NextMoveInMessageUnlocker {

    private final NextMoveMessageInRepository repo;
    private final Clock clock;
    private final NextMoveMessageMarkers nextMoveMessageMarkers;

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void unlockTimedOutMessages() {
        repo.findByLockTimeoutLessThanEqual(OffsetDateTime.now(clock)).forEach(cr -> {
            cr.setLockTimeout(null);
            repo.save(cr);
            log.info(nextMoveMessageMarkers.markerFrom(cr),
                    "Lock for message with id={} timed out, releasing", cr.getMessageId());
        });
    }
}
