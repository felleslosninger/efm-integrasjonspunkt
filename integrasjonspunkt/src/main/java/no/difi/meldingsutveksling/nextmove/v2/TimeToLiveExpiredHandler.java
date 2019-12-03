package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.receipt.*;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.ConversationRepository;
import no.difi.meldingsutveksling.status.ConversationService;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static no.difi.meldingsutveksling.status.ConversationMarker.markerFrom;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeToLiveExpiredHandler {
    private final ConversationService conversationService;
    private final ConversationRepository repo;
    private final MessageStatusFactory messageStatusFactory;
    private final NextMoveMessageInRepository inRepository;
    private final BusinessMessageFileRepository businessMessageFileRepository;
    private final MessagePersister messagePersister;

    @Transactional
    public void setExpired(Long id) {
        repo.findById(id).ifPresent(this::setExpired);
    }

    private void setExpired(Conversation conversation) {
        conversationService.registerStatus(conversation,
                messageStatusFactory.getMessageStatus(ReceiptStatus.LEVETID_UTLOPT));

        String messageId = conversation.getMessageId();

        inRepository.findIdByMessageId(messageId).ifPresent(
                id -> {
                    try {
                        messagePersister.delete(messageId);
                    } catch (IOException e) {
                        log.error(markerFrom(conversation), "Could not delete files for expired message {}", messageId, e);
                    }

                    businessMessageFileRepository.deleteFilesByMessageId(id);
                    inRepository.deleteMessageById(id);
                }
        );
    }
}
