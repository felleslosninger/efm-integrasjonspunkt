package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.kvittering.SBDReceiptFactory;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusPayload;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusPayloadConverter;
import no.difi.meldingsutveksling.noarkexchange.MessageContext;
import no.difi.meldingsutveksling.noarkexchange.MessageContextException;
import no.difi.meldingsutveksling.noarkexchange.MessageContextFactory;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE;
import static no.difi.meldingsutveksling.domain.sbdh.SBDUtil.isExpired;
import static no.difi.meldingsutveksling.nextmove.ServiceBusQueueMode.*;
import static no.difi.meldingsutveksling.nextmove.TimeToLiveHelper.registerErrorStatusAndMessage;
import static no.difi.meldingsutveksling.nextmove.TimeToLiveHelper.timeToLiveErrorMessage;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
@Slf4j
public class NextMoveServiceBus {

    private final IntegrasjonspunktProperties props;
    private final NextMoveQueue nextMoveQueue;
    private final ServiceBusRestClient serviceBusClient;
    private final ObjectMapper om;
    private final MessageContextFactory messageContextFactory;
    private final AsicHandler asicHandler;
    private final InternalQueue internalQueue;
    private final CryptoMessagePersister cryptoMessagePersister;
    private final ServiceBusPayloadConverter payloadConverter;
    private final SBDReceiptFactory sbdReceiptFactory;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final ConversationService conversationService;
    private final MessageStatusFactory messageStatusFactory;

    private IMessageReceiver messageReceiver;

    public NextMoveServiceBus(IntegrasjonspunktProperties props,
                              NextMoveQueue nextMoveQueue,
                              ServiceBusRestClient serviceBusClient,
                              ObjectMapper om,
                              MessageContextFactory messageContextFactory,
                              AsicHandler asicHandler,
                              @Lazy InternalQueue internalQueue,
                              CryptoMessagePersister cryptoMessagePersister,
                              ServiceBusPayloadConverter payloadConverter,
                              SBDReceiptFactory sbdReceiptFactory,
                              ServiceRegistryLookup serviceRegistryLookup,
                              ConversationService conversationService, MessageStatusFactory messageStatusFactory) {
        this.props = props;
        this.nextMoveQueue = nextMoveQueue;
        this.serviceBusClient = serviceBusClient;
        this.om = om;
        this.messageContextFactory = messageContextFactory;
        this.asicHandler = asicHandler;
        this.internalQueue = internalQueue;
        this.cryptoMessagePersister = cryptoMessagePersister;
        this.payloadConverter = payloadConverter;
        this.sbdReceiptFactory = sbdReceiptFactory;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.conversationService = conversationService;
        this.messageStatusFactory = messageStatusFactory;
    }

    @PostConstruct
    public void init() throws NextMoveException {
        if (props.getNextmove().getServiceBus().isBatchRead()) {
            String connectionString = String.format("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s",
                    props.getNextmove().getServiceBus().getBaseUrl(),
                    props.getNextmove().getServiceBus().getSasKeyName(),
                    serviceBusClient.getSasKey());
            ConnectionStringBuilder connectionStringBuilder = new ConnectionStringBuilder(connectionString, serviceBusClient.getLocalQueuePath());
            try {
                this.messageReceiver = ClientFactory.createMessageReceiverFromConnectionStringBuilder(connectionStringBuilder, ReceiveMode.PEEKLOCK);
            } catch (InterruptedException | ServiceBusException e) {
                throw new NextMoveException(e);
            }
        }

    }

    public void putMessage(NextMoveOutMessage message) throws NextMoveException {
        ServiceBusPayload payload = ServiceBusPayload.of(message.getSbd(), getAsicBytes(message));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            om.writeValue(bos, payload);
            serviceBusClient.sendMessage(bos.toByteArray(), getReceiverQueue(message));
        } catch (IOException e) {
            throw new NextMoveException("Error creating servicebus payload", e);
        }
    }

    private byte[] getAsicBytes(NextMoveOutMessage message) throws NextMoveException {
        byte[] asicBytes = null;
        try {
            InputStream encryptedAsic = asicHandler.createEncryptedAsic(message, getMessageContext(message));
            if (encryptedAsic != null) {
                asicBytes = Base64.getEncoder()
                        .encode(IOUtils.toByteArray(encryptedAsic));
            }
        } catch (IOException e) {
            throw new NextMoveException("Unable to read encrypted asic", e);
        }
        return asicBytes;
    }

    private MessageContext getMessageContext(NextMoveMessage message) throws NextMoveException {
        try {
            return messageContextFactory.from(message);
        } catch (MessageContextException e) {
            throw new NextMoveException("Could not create message context", e);
        }
    }

    @Transactional
    public void getAllMessagesRest() {
        boolean messagesInQueue = true;
        while (messagesInQueue) {
            ArrayList<ServiceBusMessage> messages = Lists.newArrayList();
            for (int i = 0; i < props.getNextmove().getServiceBus().getReadMaxMessages(); i++) {
                Optional<ServiceBusMessage> msg = serviceBusClient.receiveMessage();
                if (!msg.isPresent()) {
                    messagesInQueue = false;
                    break;
                }
                messages.add(msg.get());
            }

            for (ServiceBusMessage msg : messages) {
                if (msg.getPayload().getAsic() != null) {
                    try {
                        cryptoMessagePersister.write(msg.getPayload().getSbd().getConversationId(),
                                ASIC_FILE,
                                Base64.getDecoder().decode(msg.getPayload().getAsic()));
                    } catch (IOException e) {
                        throw new NextMoveRuntimeException("Error persisting ASiC, aborting..", e);
                    }
                }
                handleSbd(msg.getPayload().getSbd());
                serviceBusClient.deleteMessage(msg);

                if (isExpired(msg.getPayload().getSbd())) {
                    registerErrorStatusAndMessage(msg.getPayload().getSbd(), conversationService);
                }
            }
        }
    }

    public CompletableFuture getAllMessagesBatch() {
        return CompletableFuture.runAsync(() -> {
            boolean hasQueuedMessages = true;
            while (hasQueuedMessages) {
                try {
                    log.trace("Calling receiveBatch..");
                    Collection<IMessage> messages = messageReceiver.receiveBatch(100, Duration.ofSeconds(10));
                    if (messages != null && !messages.isEmpty()) {
                        log.debug("Processing {} messages..", messages.size());
                        messages.forEach(this::handleMessage);
                        log.debug("Done processing {} messages", messages.size());
                    } else {
                        log.trace("No messages in queue, cancelling batch");
                        hasQueuedMessages = false;
                    }
                } catch (InterruptedException | ServiceBusException e) {
                    log.error("Error while processing messages from service bus", e);
                }
            }
        });
    }

    private void handleMessage(IMessage m) {
        try {
            log.debug(format("Received message on queue=%s with id=%s", serviceBusClient.getLocalQueuePath(), m.getMessageId()));
            ServiceBusPayload payload = payloadConverter.convert(m.getBody(), m.getMessageId());
            if (payload.getAsic() != null) {
                cryptoMessagePersister.write(payload.getSbd().getConversationId(), ASIC_FILE, Base64.getDecoder().decode(payload.getAsic()));
            }
            handleSbd(payload.getSbd());
            messageReceiver.completeAsync(m.getLockToken());
        } catch (JAXBException | IOException e) {
            log.error("Failed to put message on local queue", e);
        }
    }

    private void handleSbd(StandardBusinessDocument sbd) {
        if (SBDUtil.isStatus(sbd)) {
            log.debug(String.format("Message with id=%s is a receipt", sbd.getConversationId()));
            StatusMessage msg = (StatusMessage) sbd.getAny();
            conversationService.registerStatus(sbd.getConversationId(), messageStatusFactory.getMessageStatus(msg.getStatus()))
                    .ifPresent(conversationService::markFinished);
        } else {
            sendReceiptAsync(nextMoveQueue.enqueue(sbd, DPE));
        }
    }

    private void sendReceiptAsync(NextMoveInMessage message) {
        CompletableFuture.runAsync(() -> sendReceipt(message));
    }

    private void sendReceipt(NextMoveInMessage message) {
        internalQueue.enqueueNextMove2(NextMoveOutMessage.of(getReceipt(message), DPE));
    }

    private StandardBusinessDocument getReceipt(NextMoveInMessage message) {
        return sbdReceiptFactory.createEinnsynStatusFrom(message.getSbd(),
                DocumentType.STATUS,
                ReceiptStatus.MOTTATT);
    }

    private String getReceiverQueue(NextMoveOutMessage message) {
        String prefix = NextMoveConsts.NEXTMOVE_QUEUE_PREFIX+message.getReceiverIdentifier();

        if (SBDUtil.isReceipt(message.getSbd())) {
            return prefix+receiptTarget();
        }

        try {
            ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecordByProcess(
                    message.getReceiverIdentifier(),
                    message.getSbd().getProcess());

            return prefix+serviceRecord.getService().getEndpointUrl();
        } catch (ServiceRegistryLookupException e) {
            throw new NextMoveRuntimeException(String.format("Unable to get service record for %s", message.getReceiverIdentifier()), e);
        }
    }

    private String receiptTarget() {
        if (!isNullOrEmpty(props.getNextmove().getServiceBus().getReceiptQueue())) {
            return props.getNextmove().getServiceBus().getReceiptQueue();
        }
        if (MEETING.fullname().equals(props.getNextmove().getServiceBus().getMode())) {
            return INNSYN.fullname();
        }
        if (INNSYN.fullname().equals(props.getNextmove().getServiceBus().getMode())) {
            return DATA.fullname();
        }
        return INNSYN.fullname();
    }
}
