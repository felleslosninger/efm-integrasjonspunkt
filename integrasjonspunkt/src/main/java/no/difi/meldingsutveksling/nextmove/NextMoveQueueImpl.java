package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.altinnv3.dpo.MessageChannelEntry;
import no.difi.meldingsutveksling.altinnv3.dpo.MessageChannelRepository;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.api.NextMoveQueue;
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
    public void enqueueIncomingMessage(StandardBusinessDocument sbd, ServiceIdentifier serviceIdentifier) {
        enqueueIncomingMessage(sbd, serviceIdentifier, null);
    }

    @Override
    @Transactional
    public void enqueueIncomingMessage(StandardBusinessDocument sbd,
                                       ServiceIdentifier serviceIdentifier,
                                       Resource asic) {
        MDC.put(NextMoveConsts.CORRELATION_ID, sbd.getMessageId());
        if (!(sbd.getAny() instanceof BusinessMessage)) {
            throw new MeldingsUtvekslingRuntimeException("SBD payload not of a known type");
        } else if (sbdService.isExpired(sbd)) {
            timeToLiveHelper.registerErrorStatusAndMessage(sbd, serviceIdentifier, ConversationDirection.INCOMING);
            return;
        } else if (SBDUtil.isStatus(sbd)) {
            log.debug("Message with id={} is a receipt", sbd.getMessageId());
            conversationService.registerStatus(sbd.getMessageId(), ((StatusMessage) sbd.getAny()).getStatus());
            return;
        }

        if (asic != null) {
            try {
                messagePersister.write(sbd.getMessageId(), NextMoveConsts.ASIC_FILE, asic);
            } catch (java.io.IOException e) {
                throw new MeldingsUtvekslingRuntimeException("Failed to persist ASIC for message " + sbd.getMessageId(), e);
            }
        }

        if (messageRepo.findByMessageId(sbd.getMessageId()).isEmpty()) {
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
        } else {
            log.warn("Received duplicate message with id={}, message discarded.", sbd.getMessageId());
        }
    }

}
