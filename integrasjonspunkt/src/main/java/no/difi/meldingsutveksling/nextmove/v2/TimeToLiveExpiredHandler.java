package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.ConversationRepository;
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
                        log.warn(markerFrom(conversation), "Error deleting files in TimeToLiveExpiredHandler.setExpired() for expired message {}", messageId, e);
                    }

                    businessMessageFileRepository.deleteFilesByMessageId(id);
                    inRepository.deleteMessageById(id);
                }
        );
    }
}
