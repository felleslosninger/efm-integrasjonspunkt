package no.difi.meldingsutveksling.nextmove.servicebus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.microsoft.azure.servicebus.*;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
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
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
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

    private IMessageReceiver messageReceiver;

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
            ConnectionStringBuilder connectionStringBuilder = new ConnectionStringBuilder(connectionString, serviceBusClient.getLocalQueuePath());
            try {
                this.messageReceiver = ClientFactory.createMessageReceiverFromConnectionStringBuilder(connectionStringBuilder, ReceiveMode.PEEKLOCK);
            } catch (InterruptedException e) {
                log.error("Error while constructing message receiver. Thread was interrupted", e);
                // Restore interrupted state..
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
                messages.add(msg.get());
            }

            for (ServiceBusMessage msg : messages) {
                Resource asic = getAsic(msg);
                nextMoveQueue.enqueueIncomingMessage(msg.getPayload().getSbd(), DPE, asic);
                serviceBusClient.deleteMessage(msg);
            }
        }
    }

    @Nullable
    private Resource getAsic(ServiceBusMessage msg) {
        return (msg.getPayload().getAsic() != null) ? new ByteArrayResource(Base64.getDecoder().decode(msg.getPayload().getAsic())) : null;
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
                    if (!Strings.isNullOrEmpty(e.getMessage())) {
                        log.error("Error while processing messages from service bus. Thread was interrupted", e);
                    } else {
                        log.trace("Error while processing messages from service bus. Thread was interrupted", e);
                    }
                    // Restore interrupted state..
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
            Resource asic = getAsic(payload);
            nextMoveQueue.enqueueIncomingMessage(payload.getSbd(), DPE, asic);
            messageReceiver.completeAsync(m.getLockToken());
        } catch (IOException e) {
            log.error("Failed to put message on local queue", e);
        }
    }

    @Nullable
    private Resource getAsic(ServiceBusPayload payload) {
        return (payload.getAsic() != null) ? new ByteArrayResource(Base64.getDecoder().decode(payload.getAsic())) : null;
    }

    private byte[] getBody(IMessage message) {
        MessageBody messageBody = message.getMessageBody();
        return messageBody.getBinaryData().get(0);
    }

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
