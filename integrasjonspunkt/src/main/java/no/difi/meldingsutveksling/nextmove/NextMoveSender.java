package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageOutRepository;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Optional;

import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@Component
@Slf4j
@RequiredArgsConstructor
public class NextMoveSender {

    private final ConversationStrategyFactory strategyFactory;
    private final ConversationService conversationService;
    private final CryptoMessagePersister cryptoMessagePersister;
    private final NextMoveMessageOutRepository messageRepo;

    @Transactional
    public void send(NextMoveMessage msg) throws NextMoveException {
        Optional<ConversationStrategy> strategy = strategyFactory.getStrategy(msg);
        if (!strategy.isPresent()) {
            String errorStr = String.format("Cannot send message - serviceIdentifier \"%s\" not supported",
                    msg.getServiceIdentifier());
            log.error(markerFrom(msg), errorStr);
            throw new NextMoveRuntimeException(errorStr);
        }

        strategy.get().send(msg);

        if (SBDUtil.isStatus(msg.getSbd())) {
            return;
        }

        conversationService.registerStatus(msg.getConversationId(), MessageStatus.of(ReceiptStatus.SENDT));
        messageRepo.deleteByConversationId(msg.getConversationId());
        try {
            cryptoMessagePersister.delete(msg.getConversationId());
        } catch (IOException e) {
            log.error("Error deleting files from conversation with id={}", msg.getConversationId(), e);
        }
    }
}
