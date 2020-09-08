package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.kvittering.SBDReceiptFactory;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPE;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@Slf4j
public class NextMoveQueueImpl implements NextMoveQueue {

    private final NextMoveMessageInRepository messageRepo;
    private final ConversationService conversationService;
    private final MessageStatusFactory messageStatusFactory;
    private final InternalQueue internalQueue;
    private final SBDReceiptFactory receiptFactory;
    private final SBDUtil sbdUtil;

    @Transactional
    public void enqueueIncomingMessage(StandardBusinessDocument sbd, @NotNull ServiceIdentifier serviceIdentifier) {
        if (sbd.getAny() instanceof BusinessMessage) {
            if (sbdUtil.isStatus(sbd)) {
                log.debug(String.format("Message with id=%s is a receipt", sbd.getDocumentId()));
                StatusMessage msg = (StatusMessage) sbd.getAny();
                conversationService.registerStatus(sbd.getDocumentId(), messageStatusFactory.getMessageStatus(msg.getStatus()));
                return;
            }

            NextMoveInMessage message = NextMoveInMessage.of(sbd, serviceIdentifier);
            if (!messageRepo.findByMessageId(sbd.getDocumentId()).isPresent()) {
                messageRepo.save(message);
            }

            Conversation c = conversationService.registerConversation(sbd, serviceIdentifier, ConversationDirection.INCOMING);
            conversationService.registerStatus(c, messageStatusFactory.getMessageStatus(ReceiptStatus.INNKOMMENDE_MOTTATT));

            if (DPO == serviceIdentifier) {
                StandardBusinessDocument statusSbd = receiptFactory.createArkivmeldingStatusFrom(message.getSbd(), DocumentType.STATUS, ReceiptStatus.MOTTATT);
                NextMoveOutMessage msg = NextMoveOutMessage.of(statusSbd, DPO);
                internalQueue.enqueueNextMove(msg);
            }
            if (DPE == serviceIdentifier) {
                StandardBusinessDocument statusSbd = receiptFactory.createEinnsynStatusFrom(message.getSbd(), DocumentType.STATUS, ReceiptStatus.MOTTATT);
                NextMoveOutMessage msg = NextMoveOutMessage.of(statusSbd, DPE);
                internalQueue.enqueueNextMove(msg);

            }

            Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] put on local queue",
                    message.getMessageId(), message.getServiceIdentifier()), markerFrom(message));
        } else {
            throw new MeldingsUtvekslingRuntimeException("SBD payload not of a known type");
        }
    }

}
