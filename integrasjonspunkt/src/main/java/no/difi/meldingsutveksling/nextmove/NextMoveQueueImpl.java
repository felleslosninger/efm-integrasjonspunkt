package no.difi.meldingsutveksling.nextmove;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.MessagePersister;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@Slf4j
public class NextMoveQueueImpl implements NextMoveQueue {

    private final NextMoveMessageInRepository messageRepo;
    private final ConversationService conversationService;
    private final MessageStatusFactory messageStatusFactory;
    private final InternalQueue internalQueue;
    private final SBDReceiptFactory receiptFactory;
    private final SBDUtil sbdUtil;
    private final MessagePersister messagePersister;

    public NextMoveQueueImpl(NextMoveMessageInRepository messageRepo,
                             ConversationService conversationService,
                             MessageStatusFactory messageStatusFactory,
                             @Lazy InternalQueue internalQueue,
                             SBDReceiptFactory receiptFactory,
                             SBDUtil sbdUtil,
                             MessagePersister messagePersister) {
        this.messageRepo = messageRepo;
        this.conversationService = conversationService;
        this.messageStatusFactory = messageStatusFactory;
        this.internalQueue = internalQueue;
        this.receiptFactory = receiptFactory;
        this.sbdUtil = sbdUtil;
        this.messagePersister = messagePersister;
    }

    @Transactional
    public void enqueueIncomingMessage(StandardBusinessDocument sbd, @NotNull ServiceIdentifier serviceIdentifier, InputStream asicStream) {
        if (sbd.getAny() instanceof BusinessMessage) {
            if (sbdUtil.isStatus(sbd)) {
                log.debug(String.format("Message with id=%s is a receipt", sbd.getDocumentId()));
                StatusMessage msg = (StatusMessage) sbd.getAny();
                conversationService.registerStatus(sbd.getDocumentId(), messageStatusFactory.getMessageStatus(msg.getStatus()));
                return;
            }

            if (asicStream != null) {
                try (InputStream is = asicStream) {
                    messagePersister.writeStream(sbd.getDocumentId(), ASIC_FILE, is, -1L);
                } catch (IOException e) {
                    throw new NextMoveRuntimeException("Error persisting ASiC", e);
                }
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
