package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.CryptoMessagePersister;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.*;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import no.difi.meldingsutveksling.nextmove.ResponseStatusSender;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Slf4j
@Component
@RequiredArgsConstructor
public class NextMoveMessageInService {

    private final IntegrasjonspunktProperties props;
    private final ConversationService conversationService;
    private final NextMoveMessageInRepository messageRepo;
    private final CryptoMessagePersister cryptoMessagePersister;
    private final ResponseStatusSender responseStatusSender;
    private final Clock clock;

    @Transactional
    public Page<StandardBusinessDocument> findMessages(
            NextMoveInMessageQueryInput input, Pageable pageable) {
        return messageRepo.find(input, pageable);
    }

    public Optional<NextMoveInMessage> peek(NextMoveInMessageQueryInput input) {
        OffsetDateTime lockTimeout = OffsetDateTime.now(clock)
                .plusMinutes(props.getNextmove().getLockTimeoutMinutes());

        for (Long id : messageRepo.findIdsForUnlockedMessages(input, 20)) {
            Optional<NextMoveInMessage> lockedMessage = messageRepo.lock(id, lockTimeout);
            if (lockedMessage.isPresent()) {
                return lockedMessage;
            }
        }

        throw new NoContentException();
    }

    @Transactional
    public Resource popMessage(String messageId) throws AsicPersistenceException {
        NextMoveInMessage message = messageRepo.findByMessageId(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        if (message.getLockTimeout() == null) {
            throw new MessageNotLockedException(messageId);
        }

        if (SBDUtil.isReceipt(message.getSbd())) {
            return null;
        }

        try {
            Audit.info(String.format("Pop - returning ASiC stream for message with id=%s", message.getMessageId()), markerFrom(message));
            return cryptoMessagePersister.read(messageId, ASIC_FILE);
        } catch (PersistenceException | IOException e) {
            String errorMsg = format("Can not read file \"%s\" for message [messageId=%s, sender=%s], removing from queue.",
                    ASIC_FILE, message.getMessageId(), message.getSenderIdentifier());
            Audit.error(errorMsg, markerFrom(message), e);
            messageRepo.delete(message);
            conversationService.registerStatus(messageId, ReceiptStatus.FEIL, errorMsg);
            // throw checked AsicPersistanceException so that deletion transaction is not rolled back
            throw new AsicPersistenceException();
        }
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
            log.warn("Error deleting files in NextMoveMessageInService.deleteMessage() from message with id={}", messageId, e);
        }

        messageRepo.delete(message);
        conversationService.registerStatus(messageId, ReceiptStatus.INNKOMMENDE_LEVERT);
        Audit.info(format("Message [id=%s, serviceIdentifier=%s] deleted from queue", messageId, message.getServiceIdentifier()),
                markerFrom(message));

        responseStatusSender.queue(message.getSbd(), message.getServiceIdentifier(), ReceiptStatus.LEVERT);

        return message.getSbd();
    }

    @Transactional
    public void handleCorruptMessage(String messageId) {
        NextMoveInMessage message = messageRepo.findByMessageId(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
        String errorMsg = format("Can not retrieve file \"%s\" for message [messageId=%s, sender=%s], removing from queue.",
                ASIC_FILE, message.getMessageId(), message.getSenderIdentifier());
        Audit.error(errorMsg, markerFrom(message));
        messageRepo.delete(message);
        conversationService.registerStatus(messageId, ReceiptStatus.FEIL, errorMsg);
        throw new AsicReadException(messageId);
    }
}
