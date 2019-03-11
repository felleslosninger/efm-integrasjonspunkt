package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@Slf4j
@RequiredArgsConstructor
public class ConversationResourceUnlocker {

    private final ConversationResourceRepository repo;

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void unlockTimedOutMessages() {
        List<ConversationResource> lockedResources = repo.findByLockTimeoutLessThanEqual(LocalDateTime.now());
        if (lockedResources == null || lockedResources.isEmpty()) {
            return;
        }
        lockedResources.forEach(cr -> {
            cr.setLockTimeout(null);
            repo.save(cr);
            log.debug(markerFrom(cr), "Lock for conversation with id={} timed out, releasing", cr.getConversationId());
        });
    }
}
