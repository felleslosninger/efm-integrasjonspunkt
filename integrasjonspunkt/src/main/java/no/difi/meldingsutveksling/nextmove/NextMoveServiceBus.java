package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.*;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.kvittering.SBDReceiptFactory;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusPayload;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusPayloadConverter;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.status.ConversationService;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE;
import static no.difi.meldingsutveksling.nextmove.ConversationDirection.INCOMING;
import static no.difi.meldingsutveksling.nextmove.ServiceBusQueueMode.*;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
@Slf4j
public class NextMoveServiceBus {

    private final IntegrasjonspunktProperties props;
    private final NextMoveQueue nextMoveQueue;
    private final ServiceBusRestClient serviceBusClient;
    private final ObjectMapper om;
    private final InternalQueue internalQueue;
    private final MessagePersister messagePersister;
    private final ServiceBusPayloadConverter payloadConverter;
    private final SBDReceiptFactory sbdReceiptFactory;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final ConversationService conversationService;
    private final MessageStatusFactory messageStatusFactory;
    private final TimeToLiveHelper timeToLiveHelper;
    private final SBDUtil sbdUtil;
    private final TaskExecutor taskExecutor;
    private final NextMoveServiceBusPayloadFactory nextMoveServiceBusPayloadFactory;

    private IMessageReceiver messageReceiver;

    public NextMoveServiceBus(IntegrasjonspunktProperties props,
                              NextMoveQueue nextMoveQueue,
                              ServiceBusRestClient serviceBusClient,
                              ObjectMapper om,
                              @Lazy InternalQueue internalQueue,
                              MessagePersister messagePersister,
                              ServiceBusPayloadConverter payloadConverter,
                              SBDReceiptFactory sbdReceiptFactory,
                              ServiceRegistryLookup serviceRegistryLookup,
                              ConversationService conversationService,
                              MessageStatusFactory messageStatusFactory,
                              TimeToLiveHelper timeToLiveHelper,
                              SBDUtil sbdUtil,
                              TaskExecutor taskExecutor,
                              NextMoveServiceBusPayloadFactory nextMoveServiceBusPayloadFactory) {
        this.props = props;
        this.nextMoveQueue = nextMoveQueue;
        this.serviceBusClient = serviceBusClient;
        this.om = om;
        this.messagePersister = messagePersister;
        this.nextMoveServiceBusPayloadFactory = nextMoveServiceBusPayloadFactory;
        this.internalQueue = internalQueue;
        this.payloadConverter = payloadConverter;
        this.sbdReceiptFactory = sbdReceiptFactory;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.conversationService = conversationService;
        this.messageStatusFactory = messageStatusFactory;
        this.timeToLiveHelper = timeToLiveHelper;
        this.sbdUtil = sbdUtil;
        this.taskExecutor = taskExecutor;
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
            } catch (InterruptedException e) {
                log.error("Error while constructing message receiver. Thread was interrupted", e);
                // Restore interrupted state...      T
                Thread.currentThread().interrupt();
            } catch (ServiceBusException e) {
                throw new NextMoveException(e);
            }
        }

    }

    public void putMessage(NextMoveOutMessage message) throws NextMoveException {
        ServiceBusPayload payload = nextMoveServiceBusPayloadFactory.toServiceBusPayload(message);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            om.writeValue(bos, payload);
            serviceBusClient.sendMessage(bos.toByteArray(), getReceiverQueue(message));
        } catch (IOException e) {
            throw new NextMoveException("Error creating servicebus payload", e);
        }
    }

    public void getAllMessagesRest() {
        boolean messagesInQueue = true;
        while (messagesInQueue) {
            ArrayList<ServiceBusMessage> messages = new ArrayList<>();
            for (int i = 0; i < props.getNextmove().getServiceBus().getReadMaxMessages(); i++) {
                Optional<ServiceBusMessage> msg = serviceBusClient.receiveMessage();
                if (!msg.isPresent()) {
                    messagesInQueue = false;
                    break;
                }
                if (sbdUtil.isExpired(msg.get().getPayload().getSbd())) {
                    timeToLiveHelper.registerErrorStatusAndMessage(msg.get().getPayload().getSbd(), DPE, INCOMING);
                    serviceBusClient.deleteMessage(msg.get());
                } else {
                    messages.add(msg.get());
                }
            }

            for (ServiceBusMessage msg : messages) {
                if (msg.getPayload().getAsic() != null) {
                    try {
                        messagePersister.write(msg.getPayload().getSbd().getDocumentId(),
                                ASIC_FILE,
                                Base64.getDecoder().decode(msg.getPayload().getAsic()));
                    } catch (IOException e) {
                        throw new NextMoveRuntimeException("Error persisting ASiC, aborting..", e);
                    }
                }
                handleSbd(msg.getPayload().getSbd());
                serviceBusClient.deleteMessage(msg);
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
                } catch (InterruptedException e) {
                    log.error("Error while processing messages from service bus. Thread was interrupted", e);
                    // Restore interrupted state...      T
                    Thread.currentThread().interrupt();
                } catch (ServiceBusException e) {
                    log.error("Error while processing messages from service bus", e);
                }
            }
        }, taskExecutor);
    }

    private void handleMessage(IMessage m) {
        try {
            log.debug(format("Received message on queue=%s with id=%s", serviceBusClient.getLocalQueuePath(), m.getMessageId()));
            ServiceBusPayload payload = payloadConverter.convert(getBody(m));
            if (sbdUtil.isExpired(payload.getSbd())) {
                timeToLiveHelper.registerErrorStatusAndMessage(payload.getSbd(), DPE, INCOMING);
            } else {
                if (payload.getAsic() != null) {
                    messagePersister.write(payload.getSbd().getDocumentId(), ASIC_FILE, Base64.getDecoder().decode(payload.getAsic()));
                }
                handleSbd(payload.getSbd());
            }
            messageReceiver.completeAsync(m.getLockToken());
        } catch (IOException e) {
            log.error("Failed to put message on local queue", e);
        }
    }

    private byte[] getBody(IMessage message) {
        MessageBody messageBody = message.getMessageBody();
        return messageBody.getBinaryData().get(0);
    }

    private void handleSbd(StandardBusinessDocument sbd) {
        if (sbdUtil.isStatus(sbd)) {
            log.debug(String.format("Message with id=%s is a receipt", sbd.getDocumentId()));
            StatusMessage msg = (StatusMessage) sbd.getAny();
            conversationService.registerStatus(sbd.getDocumentId(), messageStatusFactory.getMessageStatus(msg.getStatus()));
        } else {
            sendReceiptAsync(nextMoveQueue.enqueue(sbd, DPE));
        }
    }

    private void sendReceiptAsync(NextMoveInMessage message) {
        CompletableFuture.runAsync(() -> sendReceipt(message), taskExecutor);
    }

    private void sendReceipt(NextMoveInMessage message) {
        internalQueue.enqueueNextMove(NextMoveOutMessage.of(getReceipt(message), DPE));
    }

    private StandardBusinessDocument getReceipt(NextMoveInMessage message) {
        return sbdReceiptFactory.createEinnsynStatusFrom(message.getSbd(),
                DocumentType.STATUS,
                ReceiptStatus.MOTTATT);
    }

    private String getReceiverQueue(NextMoveOutMessage message) {
        String prefix = NextMoveConsts.NEXTMOVE_QUEUE_PREFIX + message.getReceiverIdentifier();

        if (sbdUtil.isStatus(message.getSbd())) {
            return prefix + receiptTarget();
        }

        try {
            ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(
                    SRParameter.builder(message.getReceiverIdentifier())
                            .process(message.getSbd().getProcess())
                            .conversationId(message.getConversationId()).build(),
                    message.getSbd().getStandard());

            if (!StringUtils.hasText(serviceRecord.getService().getEndpointUrl())) {
                throw new NextMoveRuntimeException(String.format("No endpointUrl defined for process %s", serviceRecord.getProcess()));
            }
            return prefix + serviceRecord.getService().getEndpointUrl();
        } catch (ServiceRegistryLookupException e) {
            throw new NextMoveRuntimeException(String.format("Unable to get service record for %s", message.getReceiverIdentifier()), e);
        }
    }

    private String receiptTarget() {
        if (StringUtils.hasText(props.getNextmove().getServiceBus().getReceiptQueue())) {
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
