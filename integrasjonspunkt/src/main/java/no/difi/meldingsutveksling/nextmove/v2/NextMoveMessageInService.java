package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.FileNotFoundException;
import no.difi.meldingsutveksling.exceptions.MessageNotFoundException;
import no.difi.meldingsutveksling.exceptions.MessageNotLockedException;
import no.difi.meldingsutveksling.exceptions.NoContentException;
import no.difi.meldingsutveksling.kvittering.SBDReceiptFactory;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.message.BugFix610;
import no.difi.meldingsutveksling.api.CryptoMessagePersister;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.status.ConversationService;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.OffsetDateTime;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Slf4j
@Component
@RequiredArgsConstructor
public class NextMoveMessageInService {

    private final IntegrasjonspunktProperties props;
    private final ConversationService conversationService;
    private final NextMoveMessageInRepository messageRepo;
    private final CryptoMessagePersister cryptoMessagePersister;
    private final InternalQueue internalQueue;
    private final SBDReceiptFactory receiptFactory;
    private final MessageStatusFactory messageStatusFactory;
    private final Clock clock;

    @Transactional
    public Page<StandardBusinessDocument> findMessages(
            NextMoveInMessageQueryInput input, Pageable pageable) {
        return messageRepo.find(input, pageable);
    }

    @Transactional
    public StandardBusinessDocument peek(NextMoveInMessageQueryInput input) {
        NextMoveInMessage message = messageRepo.peek(input)
                .orElseThrow(NoContentException::new);

        messageRepo.save(message.setLockTimeout(OffsetDateTime.now(clock)
                .plusMinutes(props.getNextmove().getLockTimeoutMinutes())));

        log.info(markerFrom(message), "Message with id={} locked until {}", message.getMessageId(), message.getLockTimeout());
        return message.getSbd();
    }

    @Transactional
    public InputStreamResource popMessage(String messageId) {
        NextMoveInMessage message = messageRepo.findByMessageId(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        if (message.getLockTimeout() == null) {
            throw new MessageNotLockedException(messageId);
        }

        try {
            FileEntryStream fileEntry = cryptoMessagePersister.readStream(messageId, ASIC_FILE, throwable ->
                    Audit.error(String.format("Can not read file \"%s\" for message [messageId=%s, sender=%s].",
                            ASIC_FILE, message.getMessageId(), message.getSenderIdentifier()), markerFrom(message), throwable)
            );

            return new InputStreamResource(getInputStream(fileEntry, messageId));

        } catch (PersistenceException e) {
            Audit.error(String.format("Can not read file \"%s\" for message [messageId=%s, sender=%s].",
                    ASIC_FILE, message.getMessageId(), message.getSenderIdentifier()), markerFrom(message), e);
            throw new FileNotFoundException(ASIC_FILE);
        }
    }

    private InputStream getInputStream(FileEntryStream fileEntry, String messageId) {
        if (props.getNextmove().getApplyZipHeaderPatch()) {
            try {
                return BugFix610.applyPatch(fileEntry.getInputStream(), messageId);
            } catch (IOException e) {
                throw new NextMoveRuntimeException("Could not apply patch 610 to message", e);
            }
        }

        return fileEntry.getInputStream();
    }

    @Transactional
    public StandardBusinessDocument deleteMessage(String messageId) {
        NextMoveInMessage message = messageRepo.findByMessageId(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        if (message.getLockTimeout() == null) {
            throw new MessageNotLockedException(messageId);
        }

        try {
            cryptoMessagePersister.delete(messageId);
        } catch (IOException e) {
            log.error("Error deleting files from message with id={}", messageId, e);
        }

        messageRepo.delete(message);

        conversationService.registerStatus(messageId,
                messageStatusFactory.getMessageStatus(ReceiptStatus.INNKOMMENDE_LEVERT));

        Audit.info(format("Message with id=%s popped from queue", messageId),
                markerFrom(message));

        if (message.getServiceIdentifier() == DPO) {
            StandardBusinessDocument statusSbd = receiptFactory.createArkivmeldingStatusFrom(message.getSbd(), DocumentType.STATUS, ReceiptStatus.LEVERT);
            NextMoveOutMessage msg = NextMoveOutMessage.of(statusSbd, DPO);
            internalQueue.enqueueNextMove(msg);
        }
        if (message.getServiceIdentifier() == DPE) {
            StandardBusinessDocument statusSbd = receiptFactory.createEinnsynStatusFrom(message.getSbd(), DocumentType.STATUS, ReceiptStatus.LEVERT);
            NextMoveOutMessage msg = NextMoveOutMessage.of(statusSbd, DPE);
            internalQueue.enqueueNextMove(msg);
        }

        return message.getSbd();
    }
}
