package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Payload;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@Slf4j
public class NextMoveQueue {

    private final ConversationService conversationService;
    private final NextMoveMessageInRepository messageRepo;
    private final MessageStatusFactory messageStatusFactory;

    @Transactional
    public NextMoveInMessage enqueue(StandardBusinessDocument sbd, ServiceIdentifier serviceIdentifier) {
        if (sbd.getAny() instanceof BusinessMessage) {
            NextMoveInMessage message = NextMoveInMessage.of(sbd, serviceIdentifier);

            if (!messageRepo.findByConversationId(sbd.getConversationId()).isPresent()) {
                messageRepo.save(message);
            }

            Conversation c = conversationService.registerConversation(message);
            conversationService.registerStatus(c, messageStatusFactory.getMessageStatus(ReceiptStatus.INNKOMMENDE_MOTTATT));
            Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] put on local queue",
                    message.getConversationId(), message.getServiceIdentifier()), markerFrom(message));
            return message;

        } else {
            String errorMsg = String.format("SBD payload not of known types: %s, %s", Payload.class.getName(), BusinessMessage.class.getName());
            log.error(errorMsg);
            throw new MeldingsUtvekslingRuntimeException(errorMsg);
        }
    }

}
