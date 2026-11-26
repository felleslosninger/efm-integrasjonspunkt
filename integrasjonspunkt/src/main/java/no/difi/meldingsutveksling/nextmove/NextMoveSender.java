package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.nextmove.v2.BusinessMessageFileRepository;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageOutRepository;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@Slf4j
@RequiredArgsConstructor
public class NextMoveSender {

    private final ConversationStrategyFactory strategyFactory;
    private final ConversationService conversationService;
    private final NextMoveMessageOutRepository messageRepo;
    private final BusinessMessageFileRepository businessMessageFileRepository;
    private final SBDService sbdService;
    private final TimeToLiveHelper timeToLiveHelper;
    private final MessagePersister messagePersister;

    public void send(NextMoveOutMessage msg) throws NextMoveException {
        if (!SBDUtil.isStatus(msg.getSbd()) && isAlreadySent(msg)) {
            log.warn(markerFrom(msg), "Message with id={} has already been sent - ignoring duplicate delivery from the queue",
                    msg.getMessageId());
        } else if (sbdService.isExpired(msg.getSbd())) {
            conversationService.findConversation(msg.getMessageId())
                    .ifPresent(timeToLiveHelper::registerErrorStatusAndMessage);

            if (SBDUtil.isStatus(msg.getSbd())) {
                return;
            }
        } else {
            strategyFactory.getStrategy(msg.getServiceIdentifier())
                    .orElseThrow(() -> {
                        String errorStr = "Cannot send message - serviceIdentifier \"%s\" not supported".formatted(
                                msg.getServiceIdentifier());
                        log.error(markerFrom(msg), errorStr);
                        return new NextMoveRuntimeException(errorStr);
                    }).send(msg);

            if (SBDUtil.isStatus(msg.getSbd())) {
                return;
            }

            conversationService.registerStatus(msg.getMessageId(), ReceiptStatus.SENDT);
        }

        try {
            messagePersister.delete(msg.getMessageId());
        } catch (IOException e) {
            log.warn("Error deleting files in NextMoveSender.send() from message with id={}", msg.getMessageId(), e);
        }

        messageRepo.findIdByMessageId(msg.getMessageId()).ifPresent(
                id -> {
                    businessMessageFileRepository.deleteFilesByMessageId(id);
                    messageRepo.deleteMessageById(id);
                }
        );
    }

    /**
     * A message that has been sent is deleted together with its files right after sending. If the queue delivers it
     * again (for example because the acknowledgement was lost), sending it again would either fail because the files
     * are gone - and the message would end up as FEIL after the retries are exhausted, although it was delivered - or,
     * if the clean-up did not complete, send it a second time. Instead, the message is not sent again, and only the
     * clean-up is (re)done.
     * <p>
     * SENDT is only registered after a successful send, and a new message is rejected if a conversation with the same
     * messageId already exists (see {@code NextMoveValidator}), so SENDT can not stem from another message.
     */
    private boolean isAlreadySent(NextMoveOutMessage msg) {
        return conversationService.findConversation(msg.getMessageId())
                .map(Conversation::getMessageStatuses)
                .map(statuses -> statuses.stream().anyMatch(ms -> ReceiptStatus.SENDT.name().equals(ms.getStatus())))
                .orElse(false);
    }
}
