package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageOutRepository;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
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
    private final MessageStatusFactory messageStatusFactory;
    private final SBDUtil sbdUtil;
    private final TimeToLiveHelper timeToLiveHelper;
    private final MessagePersister messagePersister;

    public void send(NextMoveOutMessage msg) throws NextMoveException {
        if (sbdUtil.isExpired(msg.getSbd())) {
            conversationService.findConversation(msg.getMessageId())
                    .ifPresent(timeToLiveHelper::registerErrorStatusAndMessage);

            if (sbdUtil.isStatus(msg.getSbd())) {
                return;
            }
        } else {
            strategyFactory.getStrategy(msg.getServiceIdentifier())
                    .orElseThrow(() -> {
                        String errorStr = String.format("Cannot send message - serviceIdentifier \"%s\" not supported",
                                msg.getServiceIdentifier());
                        log.error(markerFrom(msg), errorStr);
                        return new NextMoveRuntimeException(errorStr);
                    }).send(msg);

            if (sbdUtil.isStatus(msg.getSbd())) {
                return;
            }

            conversationService.registerStatus(msg.getMessageId(), messageStatusFactory.getMessageStatus(ReceiptStatus.SENDT));
        }

        try {
            messagePersister.delete(msg.getMessageId());
        } catch (IOException e) {
            log.error("Error deleting files from message with id={}", msg.getMessageId(), e);
        }

        messageRepo.findIdByMessageId(msg.getMessageId()).ifPresent(
                id -> {
                    businessMessageFileRepository.deleteFilesByMessageId(id);
                    messageRepo.deleteMessageById(id);
                }
        );
    }
}
