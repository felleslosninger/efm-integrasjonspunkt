package no.difi.meldingsutveksling.nextmove;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import static no.difi.meldingsutveksling.nextmove.logging.ConversationResourceMarkers.markerFrom;

@Component
@Slf4j
public class ConversationResourceUnlocker {

    private ConversationResourceRepository repo;

    @Autowired
    public ConversationResourceUnlocker(ConversationResourceRepository repo) {
        this.repo = repo;
    }

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
