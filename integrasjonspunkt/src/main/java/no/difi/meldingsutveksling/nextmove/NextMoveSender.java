package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageRepository;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Optional;

import static no.difi.meldingsutveksling.domain.sbdh.SBDUtil.isExpired;
import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;
import static no.difi.meldingsutveksling.nextmove.TimeToLiveHelper.registerErrorStatusAndMessage;
import static no.difi.meldingsutveksling.nextmove.TimeToLiveHelper.timeToLiveErrorMessage;

@Component
@Slf4j
@RequiredArgsConstructor
public class NextMoveSender {

    private final ConversationStrategyFactory strategyFactory;
    private final ConversationService conversationService;
    private final CryptoMessagePersister cryptoMessagePersister;
    private final NextMoveMessageRepository messageRepo;

    @Transactional
    public void send(NextMoveMessage msg) throws NextMoveException {
        StandardBusinessDocumentHeader header = msg.getSbd().getStandardBusinessDocumentHeader();
        if(isExpired(header)) {
            registerErrorStatusAndMessage(msg.getSbd(), conversationService);
            timeToLiveErrorMessage(header);
        }

        Optional<ConversationStrategy> strategy = strategyFactory.getStrategy(msg);
        if (!strategy.isPresent()) {
            String errorStr = String.format("Cannot send message - serviceIdentifier \"%s\" not supported",
                    msg.getServiceIdentifier());
            log.error(markerFrom(msg), errorStr);
            throw new NextMoveRuntimeException(errorStr);
        }

        strategy.get().send(msg);
        if (msg.getServiceIdentifier() == ServiceIdentifier.DPE_RECEIPT) {
            return;
        }

        conversationService.registerStatus(msg.getConversationId(), MessageStatus.of(GenericReceiptStatus.SENDT));
        messageRepo.deleteByConversationId(msg.getConversationId());
        try {
            cryptoMessagePersister.delete(msg.getConversationId());
        } catch (IOException e) {
            log.error("Error deleting files from conversation with id={}", msg.getConversationId(), e);
        }
    }
}
