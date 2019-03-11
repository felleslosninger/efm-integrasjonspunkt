package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Payload;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.kvittering.SBDReceiptFactory;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusPayload;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusPayloadConverter;
import no.difi.meldingsutveksling.noarkexchange.MessageContext;
import no.difi.meldingsutveksling.noarkexchange.MessageContextException;
import no.difi.meldingsutveksling.noarkexchange.MessageContextFactory;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentFactory;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import org.apache.commons.io.IOUtils;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
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
import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE_DATA;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE_INNSYN;
import static no.difi.meldingsutveksling.nextmove.ServiceBusQueueMode.DATA;
import static no.difi.meldingsutveksling.nextmove.ServiceBusQueueMode.INNSYN;

@Component
public class NextMoveServiceBus {

    private static final Logger log = LoggerFactory.getLogger(NextMoveServiceBus.class);

    private static final String NEXTMOVE_QUEUE_PREFIX = "nextbestqueue";

    private IntegrasjonspunktProperties props;
    private StandardBusinessDocumentFactory sbdf;
    private NextMoveQueue nextMoveQueue;
    private JAXBContext jaxbContext;
    private ServiceBusRestClient serviceBusClient;
    private IMessageReceiver messageReceiver;
    private ObjectMapper om;
    private MessageContextFactory messageContextFactory;
    private AsicHandler asicHandler;
    private InternalQueue internalQueue;
    private MessagePersister messagePersister;
    private ServiceBusPayloadConverter payloadConverter;

    public NextMoveServiceBus(IntegrasjonspunktProperties props,
                              StandardBusinessDocumentFactory sbdf,
                              NextMoveQueue nextMoveQueue,
                              ServiceBusRestClient serviceBusClient,
                              ObjectMapper om,
                              MessageContextFactory messageContextFactory,
                              AsicHandler asicHandler,
                              @Lazy InternalQueue internalQueue,
                              ObjectProvider<MessagePersister> messagePersister,
                              ServiceBusPayloadConverter payloadConverter) throws JAXBException {
        this.props = props;
        this.sbdf = sbdf;
        this.nextMoveQueue = nextMoveQueue;
        this.serviceBusClient = serviceBusClient;
        this.om = om;
        this.messageContextFactory = messageContextFactory;
        this.asicHandler = asicHandler;
        this.internalQueue = internalQueue;
        this.messagePersister = messagePersister.getIfUnique();
        this.payloadConverter = payloadConverter;
        this.jaxbContext = JAXBContextFactory.createContext(new Class[]{StandardBusinessDocument.class, Payload.class, ConversationResource.class}, null);
    }

    @PostConstruct
    public void init() throws NextMoveException {
        if (props.getNextmove().getServiceBus().isBatchRead()) {
            String connectionString = String.format("Endpoint=sb://%s.%s/;SharedAccessKeyName=%s;SharedAccessKey=%s",
                    props.getNextmove().getServiceBus().getNamespace(),
                    props.getNextmove().getServiceBus().getHost(),
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

    public void putMessage(NextMoveMessage message) throws NextMoveException {
        MessageContext messageContext;
        try {
            messageContext = messageContextFactory.from(message);
        } catch (MessageContextException e) {
            throw new NextMoveException("Could not create message context", e);
        }
        byte[] asicBytes = null;
        try {
            InputStream encryptedAsic = asicHandler.createEncryptedAsic(message, messageContext);
            if (encryptedAsic != null) {
                asicBytes = Base64.getEncoder()
                        .encode(IOUtils.toByteArray(encryptedAsic));
            }
        } catch (IOException e) {
            throw new NextMoveException("Unable to read encrypted asic", e);
        }

        ServiceBusPayload payload = ServiceBusPayload.of(message.getSbd(), asicBytes);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            om.writeValue(bos, payload);
            String queue = getQueue(message);
            serviceBusClient.sendMessage(bos.toByteArray(), queue);
        } catch (IOException e) {
            throw new NextMoveException("Error creating servicebus payload", e);
        }

    }

    private String getQueue(NextMoveMessage message) throws NextMoveException {
        String queue = NEXTMOVE_QUEUE_PREFIX + message.getReceiverIdentifier();
        switch (message.getServiceIdentifier()) {
            case DPE_INNSYN:
                queue = queue + INNSYN.fullname();
                break;
            case DPE_DATA:
                queue = queue + DATA.fullname();
                break;
            case DPE_RECEIPT:
                queue = queue + receiptTarget();
                break;
            default:
                throw new NextMoveException("ServiceBus has no queue for ServiceIdentifier=" + message.getServiceIdentifier());
        }
        return queue;
    }

    public void getAllMessagesRest() {
        boolean messagesInQueue = true;
        while (messagesInQueue) {
            ArrayList<ServiceBusMessage> messages = Lists.newArrayList();
            for (int i=0; i<props.getNextmove().getServiceBus().getReadMaxMessages(); i++) {
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
                        messagePersister.write(msg.getPayload().getSbd().getConversationId(),
                                ASIC_FILE,
                                Base64.getDecoder().decode(msg.getPayload().getAsic()));
                    } catch (IOException e) {
                        throw new NextMoveRuntimeException("Error persisting ASiC, aborting..", e);
                    }
                }
                Optional<NextMoveMessage> cr = nextMoveQueue.enqueue(msg.getPayload().getSbd());
                cr.ifPresent(this::sendReceipt);
                serviceBusClient.deleteMessage(msg);
            }
        }
    }

    public CompletableFuture getAllMessagesBatch() {
        return CompletableFuture.runAsync(() -> {
            boolean hasQueuedMessages = true;
            while (hasQueuedMessages) {
                try {
                    log.debug("Calling receiveBatch..");
                    Collection<IMessage> messages = messageReceiver.receiveBatch(100, Duration.ofSeconds(10));
                    if (messages != null && !messages.isEmpty()) {
                        log.debug("Processing {} messages..", messages.size());
                        messages.forEach(m -> {
                            try {
                                log.debug(format("Received message on queue=%s with id=%s", serviceBusClient.getLocalQueuePath(), m.getMessageId()));
                                ServiceBusPayload payload = payloadConverter.convert(m.getBody(), m.getMessageId());
                                if (payload.getAsic() != null) {
                                    messagePersister.write(payload.getSbd().getConversationId(), ASIC_FILE, Base64.getDecoder().decode(payload.getAsic()));
                                }
                                Optional<NextMoveMessage> message = nextMoveQueue.enqueue(payload.getSbd());
                                message.ifPresent(this::sendReceiptAsync);
                                messageReceiver.completeAsync(m.getLockToken());
                            } catch (JAXBException | IOException e) {
                                log.error("Failed to put message on local queue", e);
                            }
                        });
                        log.debug("Done processing {} messages", messages.size());
                    } else {
                        log.debug("No messages in queue, cancelling batch");
                        hasQueuedMessages = false;
                    }
                } catch (InterruptedException | ServiceBusException e) {
                    log.error("Error while processing messages from service bus", e);
                }
            }
        });
    }

    private CompletableFuture sendReceiptAsync(NextMoveMessage message) {
        return CompletableFuture.runAsync(() -> sendReceipt(message));
    }

    private void sendReceipt(NextMoveMessage message) {
        if (asList(DPE_INNSYN, DPE_DATA).contains(message.getServiceIdentifier())) {
            StandardBusinessDocument sbdReceipt = SBDReceiptFactory.createDpeReceiptFrom(message.getSbd());
            internalQueue.enqueueNextMove2(NextMoveMessage.of(sbdReceipt));
        }
    }

    private String receiptTarget() {
        if (!isNullOrEmpty(props.getNextmove().getServiceBus().getReceiptQueue())) {
            return props.getNextmove().getServiceBus().getReceiptQueue();
        }
        if (INNSYN.fullname().equals(props.getNextmove().getServiceBus().getMode())) {
            return DATA.fullname();
        }
        return INNSYN.fullname();
    }
}
