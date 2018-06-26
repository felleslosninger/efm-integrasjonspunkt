package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Lists;
import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.MessageContext;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentFactory;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.*;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;
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
    private MessageSender messageSender;
    private NextMoveQueue nextMoveQueue;
    private JAXBContext jaxbContext;
    private ServiceBusRestClient serviceBusClient;
    private IMessageReceiver messageReceiver;
    private InternalQueue internalQueue;

    @Autowired
    public NextMoveServiceBus(IntegrasjonspunktProperties props,
                              StandardBusinessDocumentFactory sbdf,
                              MessageSender messageSender,
                              NextMoveQueue nextMoveQueue,
                              ServiceBusRestClient serviceBusClient,
                              @Lazy InternalQueue internalQueue) throws JAXBException {
        this.props = props;
        this.sbdf = sbdf;
        this.messageSender = messageSender;
        this.nextMoveQueue = nextMoveQueue;
        this.serviceBusClient = serviceBusClient;
        this.internalQueue = internalQueue;
        this.jaxbContext = JAXBContextFactory.createContext(new Class[]{EduDocument.class, Payload.class, ConversationResource.class}, null);
    }

    @PostConstruct
    public void init() throws NextMoveException {
        if (props.getNextmove().getServiceBus().isBatchRead()) {
            String connectionString = String.format("Endpoint=sb://%s.servicebus.windows.net/;SharedAccessKeyName=%s;SharedAccessKey=%s",
                    props.getNextmove().getServiceBus().getNamespace(),
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

    public void putMessage(ConversationResource resource) throws NextMoveException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            MessageContext context = messageSender.createMessageContext(resource);
            EduDocument eduDocument = sbdf.create(resource, context);

            Marshaller marshaller = jaxbContext.createMarshaller();

            ObjectFactory of = new ObjectFactory();
            JAXBElement<EduDocument> sbd = of.createStandardBusinessDocument(eduDocument);
            marshaller.marshal(sbd, os);

            String queue = NEXTMOVE_QUEUE_PREFIX + resource.getReceiverId();
            switch (resource.getServiceIdentifier()) {
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
                    throw new NextMoveException("ServiceBus has no queue for ServiceIdentifier=" + resource.getServiceIdentifier());
            }
            serviceBusClient.sendMessage(os.toByteArray(), queue);

        } catch (JAXBException | IOException | MessageException e) {
            log.error("Could not send conversation resource", e);
            throw new NextMoveException("Caught exception during message sending:", e);
        }
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
                try {
                    Optional<ConversationResource> cr = nextMoveQueue.enqueueEduDocument(msg.getBody());
                    cr.ifPresent(this::sendReceipt);
                    serviceBusClient.deleteMessage(msg);
                } catch (IOException e) {
                    log.error("Failed to put message on local queue", e);
                }
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
                                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                                EduDocument eduDocument = unmarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(m.getBody())), EduDocument.class).getValue();
                                Optional<ConversationResource> cr = nextMoveQueue.enqueueEduDocument(eduDocument);
                                cr.ifPresent(this::sendReceiptAsync);
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

    private CompletableFuture sendReceiptAsync(ConversationResource cr) {
        return CompletableFuture.runAsync(() -> sendReceipt(cr));
    }

    private void sendReceipt(ConversationResource cr) {
        if (asList(DPE_INNSYN, DPE_DATA).contains(cr.getServiceIdentifier())) {
            DpeReceiptConversationResource dpeReceipt = DpeReceiptConversationResource.of(cr);
            internalQueue.enqueueNextmove(dpeReceipt);
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
