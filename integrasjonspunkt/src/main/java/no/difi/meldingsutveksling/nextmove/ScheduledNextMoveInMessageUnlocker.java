package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;

@RequiredArgsConstructor
public class ScheduledNextMoveInMessageUnlocker {

    private final NextMoveInMessageUnlocker nextMoveInMessageUnlocker;

    @Scheduled(fixedDelay = 5000)
    public void unlockTimedOutMessages() {
        nextMoveInMessageUnlocker.unlockTimedOutMessages();
    }
}
