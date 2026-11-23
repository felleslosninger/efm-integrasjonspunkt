package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.altinnv3.dpo.MessageChannelEntry;
import no.difi.meldingsutveksling.altinnv3.dpo.MessageChannelRepository;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.domain.BusinessMessage;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.NextMoveMessageMarkers;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Component
public class NextMoveQueueImpl implements NextMoveQueue {

    private final NextMoveMessageInRepository messageRepo;
    private final ConversationService conversationService;
    private final SBDService sbdService;
    private final MessagePersister messagePersister;
    private final TimeToLiveHelper timeToLiveHelper;
    private final ResponseStatusSender statusSender;
    private final MessageChannelRepository messageChannelRepository;

    private final Logger log = LoggerFactory.getLogger(NextMoveQueueImpl.class);

    public NextMoveQueueImpl(
            NextMoveMessageInRepository messageRepo,
            ConversationService conversationService,
            SBDService sbdService,
            MessagePersister messagePersister,
            TimeToLiveHelper timeToLiveHelper,
            ResponseStatusSender statusSender,
            MessageChannelRepository messageChannelRepository) {
        this.messageRepo = messageRepo;
        this.conversationService = conversationService;
        this.sbdService = sbdService;
        this.messagePersister = messagePersister;
        this.timeToLiveHelper = timeToLiveHelper;
        this.statusSender = statusSender;
        this.messageChannelRepository = messageChannelRepository;
    }

    @Override
    @Transactional
    public EnqueueResult enqueueIncomingMessage(StandardBusinessDocument sbd, ServiceIdentifier serviceIdentifier) {
        return enqueueIncomingMessage(sbd, serviceIdentifier, null);
    }

    @Override
    @Transactional
    public EnqueueResult enqueueIncomingMessage(StandardBusinessDocument sbd,
                                                ServiceIdentifier serviceIdentifier,
                                                Resource asic) {
        MDC.put(NextMoveConsts.CORRELATION_ID, sbd.getMessageId());
        if (!(sbd.getAny() instanceof BusinessMessage)) {
            throw new MeldingsUtvekslingRuntimeException("SBD payload not of a known type");
        } else if (sbdService.isExpired(sbd)) {
            timeToLiveHelper.registerErrorStatusAndMessage(sbd, serviceIdentifier, ConversationDirection.INCOMING);
            discard(sbd.getMessageId(), asic);
            return EnqueueResult.EXPIRED;
        } else if (SBDUtil.isStatus(sbd)) {
            log.debug("Message with id={} is a receipt", sbd.getMessageId());
            conversationService.registerStatus(sbd.getMessageId(), ((StatusMessage) sbd.getAny()).getStatus());
            discard(sbd.getMessageId(), asic);
            return EnqueueResult.STATUS;
        }

        if (messageRepo.findByMessageId(sbd.getMessageId()).isPresent()) {
            // The ASiC of the message already on the queue is left alone, since the fagsystem may be reading it
            log.warn("Received duplicate message with id={}, message discarded.", sbd.getMessageId());
            discard(sbd.getMessageId(), asic);
            return EnqueueResult.DUPLICATE;
        }

        if (asic != null) {
            try {
                messagePersister.write(sbd.getMessageId(), NextMoveConsts.ASIC_FILE, asic);
            } catch (IOException e) {
                throw new MeldingsUtvekslingRuntimeException("Failed to persist ASIC for message " + sbd.getMessageId(), e);
            }
        }

        NextMoveInMessage message = messageRepo.save(NextMoveInMessage.of(sbd, serviceIdentifier));

        SBDUtil.getOptionalMessageChannel(sbd).ifPresent(mc ->
                messageChannelRepository.save(new MessageChannelEntry(sbd.getMessageId(), mc.getIdentifier()))
        );
        conversationService.registerConversation(
                sbd,
                serviceIdentifier,
                ConversationDirection.INCOMING,
                ReceiptStatus.INNKOMMENDE_MOTTATT
        );
        statusSender.queue(message.getSbd(), serviceIdentifier, ReceiptStatus.MOTTATT);
        log.info(NextMoveMessageMarkers.markerFrom(message),
                "Message [id={}, serviceIdentifier={}] put on local queue", message.getMessageId(), serviceIdentifier);
        return EnqueueResult.ENQUEUED;
    }

    /**
     * Reads the ASiC of a message that is not put on the queue to the end. An ASiC may be streamed while it is created, e.g. from Fiks
     * SvarInn, and the threads creating it would wait forever for it to be read.
     */
    private void discard(String messageId, Resource asic) {
        if (asic == null) {
            return;
        }
        try (InputStream inputStream = asic.getInputStream()) {
            inputStream.transferTo(OutputStream.nullOutputStream());
        } catch (IOException e) {
            log.warn("Reading the ASiC of message with id={} to the end failed", messageId, e);
        }
    }

}
