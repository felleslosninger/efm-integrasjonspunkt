package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;

import javax.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;

import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@Slf4j
@RequiredArgsConstructor
public class NextMoveInMessageUnlocker {

    private final NextMoveMessageInRepository repo;
    private final Clock clock;

    @Transactional
    public void unlockTimedOutMessages() {
        repo.findByLockTimeoutLessThanEqual(LocalDateTime.now(clock)).forEach(cr -> {
            cr.setLockTimeout(null);
            repo.save(cr);
            log.debug(markerFrom(cr), "Lock for message with id={} timed out, releasing", cr.getConversationId());
        });
    }
}
