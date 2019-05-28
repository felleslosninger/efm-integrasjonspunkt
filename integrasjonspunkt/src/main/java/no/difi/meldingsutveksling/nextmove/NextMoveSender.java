package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageOutRepository;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.IOException;

import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@Component
@Slf4j
@RequiredArgsConstructor
public class NextMoveSender {

    private final ConversationStrategyFactory strategyFactory;
    private final ConversationService conversationService;
    private final CryptoMessagePersister cryptoMessagePersister;
    private final NextMoveMessageOutRepository messageRepo;
    private final MessageStatusFactory messageStatusFactory;
    private final SBDUtil sbdUtil;
    private final TimeToLiveHelper timeToLiveHelper;

    @Transactional
    public void send(NextMoveOutMessage msg) throws NextMoveException {
        if (sbdUtil.isExpired(msg.getSbd())) {
            timeToLiveHelper.registerErrorStatusAndMessage(msg.getSbd(), msg.getServiceIdentifier(), msg.getDirection());

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

            conversationService.registerStatus(msg.getConversationId(), messageStatusFactory.getMessageStatus(ReceiptStatus.SENDT));
        }

        messageRepo.deleteByConversationId(msg.getConversationId());
        try {
            cryptoMessagePersister.delete(msg.getConversationId());
        } catch (IOException e) {
            log.error("Error deleting files from conversation with id={}", msg.getConversationId(), e);
        }
    }
}
