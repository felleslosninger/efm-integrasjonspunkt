package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.receipt.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;

import static no.difi.meldingsutveksling.receipt.ConversationMarker.markerFrom;

@Component
@Slf4j
@RequiredArgsConstructor
public class TimeToLiveTasks {
    private final ConversationService conversationService;
    private final ConversationRepository repo;
    private final MessageStatusFactory messageStatusFactory;
    private final NextMoveMessageInRepository inRepository;
    private final MessagePersister messagePersister;
    private final Clock clock;

    @Scheduled(fixedRateString = "${difi.move.nextmove.ttlPollingrate}")
    @Transactional
    public void checkExpiredMessages() {
        repo.findAll(QConversation.conversation.expiry.before(OffsetDateTime.now(clock))
                .and(QConversation.conversation.finished.isFalse()))
                .forEach(this::setExpired);
    }

    private void setExpired(Conversation conversation) {
        conversationService.registerStatus(conversation,
                messageStatusFactory.getMessageStatus(ReceiptStatus.LEVETID_UTLOPT));

        inRepository.findByConversationId(conversation.getConversationId()).ifPresent(c -> {
            try {
                messagePersister.delete(c.getConversationId());
            } catch (IOException e) {
                log.error(markerFrom(conversation), "Could not delete files for expired message {}",  c.getConversationId(), e);
            }
            inRepository.delete(c);
        });
    }
}
