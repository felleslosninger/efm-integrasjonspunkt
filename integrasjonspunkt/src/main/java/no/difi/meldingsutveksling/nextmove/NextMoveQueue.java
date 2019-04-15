package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Payload;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.Optional;

import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@Slf4j
public class NextMoveQueue {

    private final ConversationService conversationService;
    private final NextMoveMessageInRepository messageRepo;

    @Transactional
    public Optional<NextMoveMessage> enqueue(StandardBusinessDocument sbd, ServiceIdentifier serviceIdentifier) {
        if (sbd.getAny() instanceof BusinessMessage) {
            NextMoveInMessage message = NextMoveInMessage.of(sbd, serviceIdentifier);

            // TODO handle all receipts
            if (SBDUtil.isReceipt(sbd)) {
                handleDpeReceipt(message.getConversationId());
                return Optional.empty();
            }

            if(!messageRepo.findByConversationId(sbd.getConversationId()).isPresent()) {
                messageRepo.save(message);
            }

            Conversation c = conversationService.registerConversation(message);
            conversationService.registerStatus(c, MessageStatus.of(ReceiptStatus.INNKOMMENDE_MOTTATT));
            Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] put on local queue",
                    message.getConversationId(), message.getServiceIdentifier()), markerFrom(message));
            return Optional.of(message);

        } else {
            String errorMsg = String.format("SBD payload not of known types: %s, %s", Payload.class.getName(), BusinessMessage.class.getName());
            log.error(errorMsg);
            throw new MeldingsUtvekslingRuntimeException(errorMsg);
        }
    }

    private void handleDpeReceipt(String conversationId) {
        log.debug(String.format("Message with id=%s is a receipt", conversationId));
        Optional<Conversation> c = conversationService.registerStatus(conversationId, MessageStatus.of(ReceiptStatus.LEVERT));
        c.ifPresent(conversationService::markFinished);
    }
}
