package no.difi.meldingsutveksling.nextmove.servicebus;

import com.azure.messaging.servicebus.*;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE;
import static no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusQueueMode.*;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
@Slf4j
public class NextMoveServiceBus {

    private final IntegrasjonspunktProperties props;
    private final NextMoveQueue nextMoveQueue;
    private final ServiceBusRestClient serviceBusClient;
    private final ObjectMapper om;
    private final ServiceBusPayloadConverter payloadConverter;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final TaskExecutor taskExecutor;
    private final NextMoveServiceBusPayloadFactory nextMoveServiceBusPayloadFactory;

//    private IMessageReceiver messageReceiver;
    private ServiceBusProcessorClient serviceBusProcessorClient;

    public NextMoveServiceBus(IntegrasjonspunktProperties props,
                              NextMoveQueue nextMoveQueue,
                              ServiceBusRestClient serviceBusClient,
                              ObjectMapper om,
                              ServiceBusPayloadConverter payloadConverter,
                              ServiceRegistryLookup serviceRegistryLookup,
                              TaskExecutor taskExecutor,
                              NextMoveServiceBusPayloadFactory nextMoveServiceBusPayloadFactory) {
        this.props = props;
        this.nextMoveQueue = nextMoveQueue;
        this.serviceBusClient = serviceBusClient;
        this.om = om;
        this.nextMoveServiceBusPayloadFactory = nextMoveServiceBusPayloadFactory;
        this.payloadConverter = payloadConverter;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.taskExecutor = taskExecutor;
    }

    @PostConstruct
    public void init() throws NextMoveException {
        if (props.getNextmove().getServiceBus().isBatchRead()) {
            String connectionString = String.format("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s",
                props.getNextmove().getServiceBus().getBaseUrl(),
                props.getNextmove().getServiceBus().getSasKeyName(),
                serviceBusClient.getSasKey());

            serviceBusProcessorClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .queueName(serviceBusClient.getLocalQueuePath())
                .maxConcurrentCalls(50)
                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .disableAutoComplete()
                .processMessage(this::processMessage)
                .processError(this::processError)
                .buildProcessorClient();
            serviceBusProcessorClient.start();

//            ConnectionStringBuilder connectionStringBuilder = new ConnectionStringBuilder(connectionString, serviceBusClient.getLocalQueuePath());
//            try {
//                this.messageReceiver = ClientFactory.createMessageReceiverFromConnectionStringBuilder(connectionStringBuilder, ReceiveMode.PEEKLOCK);
//            } catch (InterruptedException e) {
//                log.error("Error while constructing message receiver. Thread was interrupted", e);
//                // Restore interrupted state..
//                Thread.currentThread().interrupt();
//            } catch (ServiceBusException e) {
//                throw new NextMoveException(e);
//            }
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
                messages.add(msg.get());
            }

            for (ServiceBusMessage msg : messages) {
                InputStream asicStream = (msg.getPayload().getAsic() != null) ? new ByteArrayInputStream(Base64.getDecoder().decode(msg.getPayload().getAsic())) : null;
                nextMoveQueue.enqueueIncomingMessage(msg.getPayload().getSbd(), DPE, asicStream);
                serviceBusClient.deleteMessage(msg);
            }
        }
    }
//
//    public CompletableFuture getAllMessagesBatch() {
//        return CompletableFuture.runAsync(() -> {
//            boolean hasQueuedMessages = true;
//            while (hasQueuedMessages) {
//                try {
//                    log.trace("Calling receiveBatch..");
//                    Collection<IMessage> messages = messageReceiver.receiveBatch(100, Duration.ofSeconds(10));
//                    if (messages != null && !messages.isEmpty()) {
//                        log.debug("Processing {} messages..", messages.size());
//                        messages.forEach(this::handleMessage);
//                        log.debug("Done processing {} messages", messages.size());
//                    } else {
//                        log.trace("No messages in queue, cancelling batch");
//                        hasQueuedMessages = false;
//                    }
//                } catch (InterruptedException e) {
//                    if (!Strings.isNullOrEmpty(e.getMessage())) {
//                        log.error("Error while processing messages from service bus. Thread was interrupted", e);
//                    } else {
//                        log.trace("Error while processing messages from service bus. Thread was interrupted", e);
//                    }
//                    // Restore interrupted state..
//                    Thread.currentThread().interrupt();
//                } catch (ServiceBusException e) {
//                    log.error("Error while processing messages from service bus", e);
//                }
//            }
//        }, taskExecutor);
//    }

    private void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage m = context.getMessage();
        log.debug(format("Received message on queue=%s with id=%s", serviceBusClient.getLocalQueuePath(), m.getMessageId()));
        ServiceBusPayload payload = null;
        try {
            payload = payloadConverter.convert(m.getBody().toBytes());
        } catch (IOException e) {
            log.error(String.format("Failed to convert servicebus message with id = %s, abandoning", m.getMessageId()), e);
            context.deadLetter();
            return;
        }
        InputStream asicStream = (payload.getAsic() != null) ? new ByteArrayInputStream(Base64.getDecoder().decode(payload.getAsic())) : null;
        nextMoveQueue.enqueueIncomingMessage(payload.getSbd(), DPE, asicStream);
        context.complete();
    }

    private void processError(ServiceBusErrorContext context) {
        System.out.printf("Error when receiving messages from namespace: '%s'. Entity: '%s'%n",
            context.getFullyQualifiedNamespace(), context.getEntityPath());

        if (!(context.getException() instanceof ServiceBusException)) {
            System.out.printf("Non-ServiceBusException occurred: %s%n", context.getException());
            return;
        }

        ServiceBusException exception = (ServiceBusException) context.getException();
        ServiceBusFailureReason reason = exception.getReason();

        if (reason == ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED
            || reason == ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND
            || reason == ServiceBusFailureReason.UNAUTHORIZED) {
            System.out.printf("An unrecoverable error occurred. Stopping processing with reason %s: %s%n",
                reason, exception.getMessage());

        } else if (reason == ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
            System.out.printf("Message lock lost for message: %s%n", context.getException());
        } else if (reason == ServiceBusFailureReason.SERVICE_BUSY) {
            try {
                // Choosing an arbitrary amount of time to wait until trying again.
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                System.err.println("Unable to sleep for period of time");
            }
        } else {
            System.out.printf("Error source %s, reason %s, message: %s%n", context.getErrorSource(),
                reason, context.getException());
        }
    }

//    private void handleMessage(IMessage m) {
//        try {
//            log.debug(format("Received message on queue=%s with id=%s", serviceBusClient.getLocalQueuePath(), m.getMessageId()));
//            ServiceBusPayload payload = payloadConverter.convert(getBody(m));
//            InputStream asicStream = (payload.getAsic() != null) ? new ByteArrayInputStream(Base64.getDecoder().decode(payload.getAsic())) : null;
//            nextMoveQueue.enqueueIncomingMessage(payload.getSbd(), DPE, asicStream);
//            messageReceiver.completeAsync(m.getLockToken());
//        } catch (IOException e) {
//            log.error("Failed to put message on local queue", e);
//        }
//    }
//
//    private byte[] getBody(IMessage message) {
//        MessageBody messageBody = message.getMessageBody();
//        return messageBody.getBinaryData().get(0);
//    }

    private String getReceiverQueue(NextMoveOutMessage message) {
        String prefix = NextMoveConsts.NEXTMOVE_QUEUE_PREFIX + message.getReceiverIdentifier();

        if (SBDUtil.isStatus(message.getSbd())) {
            return prefix + statusSuffix();
        }
        if (message.getBusinessMessage() instanceof EinnsynKvitteringMessage) {
            return prefix + receiptSuffix((EinnsynKvitteringMessage) message.getBusinessMessage());
        }

        try {
            ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(
                    SRParameter.builder(message.getReceiverIdentifier())
                            .process(message.getSbd().getProcess())
                            .conversationId(message.getConversationId()).build(),
                    message.getSbd().getDocumentType());

            if (!StringUtils.hasText(serviceRecord.getService().getEndpointUrl())) {
                throw new NextMoveRuntimeException(String.format("No endpointUrl defined for process %s", serviceRecord.getProcess()));
            }
            return prefix + serviceRecord.getService().getEndpointUrl();
        } catch (ServiceRegistryLookupException e) {
            throw new NextMoveRuntimeException(e);
        }
    }

    private String receiptSuffix(EinnsynKvitteringMessage k) {
        if (StringUtils.hasText(props.getNextmove().getServiceBus().getReceiptQueue())) {
            return props.getNextmove().getServiceBus().getReceiptQueue();
        }
        if (k.getReferanseType() == EinnsynType.INNSYNSKRAV) {
            return DATA.fullname();
        }
        return INNSYN.fullname();
    }

    private String statusSuffix() {
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
