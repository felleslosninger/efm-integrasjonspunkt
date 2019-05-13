package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;

@RequiredArgsConstructor
public class ScheduledConversationResourceUnlocker {

    private final ConversationResourceUnlocker conversationResourceUnlocker;

    @Scheduled(fixedDelay = 5000)
    public void unlockTimedOutMessages() {
        conversationResourceUnlocker.unlockTimedOutMessages();
    }
}
