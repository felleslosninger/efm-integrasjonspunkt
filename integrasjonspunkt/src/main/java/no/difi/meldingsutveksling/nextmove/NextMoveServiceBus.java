package no.difi.meldingsutveksling.nextmove;

import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.services.servicebus.ServiceBusConfiguration;
import com.microsoft.windowsazure.services.servicebus.ServiceBusContract;
import com.microsoft.windowsazure.services.servicebus.ServiceBusService;
import com.microsoft.windowsazure.services.servicebus.models.*;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.MessageContext;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentFactory;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.assertj.core.util.Lists;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE_DATA;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE_INNSYN;
import static no.difi.meldingsutveksling.nextmove.ServiceBusQueueMode.DATA;
import static no.difi.meldingsutveksling.nextmove.ServiceBusQueueMode.INNSYN;
import static no.difi.meldingsutveksling.nextmove.logging.ConversationResourceMarkers.markerFrom;

@Component
public class NextMoveServiceBus {

    private static final Logger log = LoggerFactory.getLogger(NextMoveServiceBus.class);

    private static final String NEXTMOVE_QUEUE_PREFIX = "nextbestqueue";

    private IntegrasjonspunktProperties props;
    private ServiceRegistryLookup sr;
    private StandardBusinessDocumentFactory sbdf;
    private MessageSender messageSender;
    private NextMoveQueue nextMoveQueue;
    private JAXBContext jaxbContext;
    private String queuePath;

    @Autowired
    public NextMoveServiceBus(IntegrasjonspunktProperties props,
                              StandardBusinessDocumentFactory sbdf,
                              ServiceRegistryLookup sr,
                              MessageSender messageSender,
                              NextMoveQueue nextMoveQueue) throws JAXBException {
        this.props = props;
        this.sbdf = sbdf;
        this.sr = sr;
        this.messageSender = messageSender;
        this.nextMoveQueue = nextMoveQueue;
        this.jaxbContext = JAXBContextFactory.createContext(new Class[]{EduDocument.class, Payload.class, ConversationResource.class}, null);
    }

    @PostConstruct
    private void init() throws ServiceException {

        if (!props.getNextbest().getServiceBus().isEnable()) {
            return;
        }

        // Create queue if it does not already exist
        ServiceBusContract service = createContract();
        queuePath = format("%s%s%s", NEXTMOVE_QUEUE_PREFIX,
                props.getOrg().getNumber(),
                props.getNextbest().getServiceBus().getMode());
        ListQueuesResult queues = service.listQueues();
        if (!queues.getItems().stream().anyMatch(i -> i.getPath().contains(queuePath))) {
            log.info("Queue with id {} does not already exist, creating it..", queuePath);
            QueueInfo qi = new QueueInfo(queuePath);
            service.createQueue(qi);
        }
    }

    public void putMessage(ConversationResource resource) throws NextMoveException {

        ServiceBusContract service = createContract();
        BrokeredMessage msg;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            MessageContext context = messageSender.createMessageContext(resource);
            EduDocument eduDocument = sbdf.create(resource, context);

            Marshaller marshaller = jaxbContext.createMarshaller();

            ObjectFactory of = new ObjectFactory();
            JAXBElement<EduDocument> sbd = of.createStandardBusinessDocument(eduDocument);
            marshaller.marshal(sbd, os);

            msg = new BrokeredMessage(os.toByteArray());

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
            service.sendMessage(queue, msg);
        } catch (ServiceException | MessageException | JAXBException | IOException e) {
            log.error("Could not send conversation resource", e);
            throw new NextMoveException(e);
        }

    }

    public void getAllMessages() throws NextMoveException {

        ReceiveMessageOptions opts = ReceiveMessageOptions.DEFAULT;
        opts.setReceiveMode(ReceiveMode.PEEK_LOCK);

        ServiceBusContract service = createContract();
        ArrayList<BrokeredMessage> messages = Lists.newArrayList();
        while (true) {
            try {
                BrokeredMessage msg = service.receiveQueueMessage(queuePath, opts).getValue();
                if (msg == null || isNullOrEmpty(msg.getMessageId())) {
                    break;
                }
                log.debug(format("Received message on queue=%s with id=%s", queuePath, msg.getMessageId()));
                messages.add(msg);

            } catch (ServiceException e) {
                log.error("Failed to fetch new message", e);
            }
        }

        for (BrokeredMessage msg : messages) {
            try {
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                EduDocument eduDocument = ((JAXBElement<EduDocument>) unmarshaller.unmarshal(msg.getBody())).getValue();
                Optional<ConversationResource> cr = nextMoveQueue.enqueueEduDocument(eduDocument);
                cr.ifPresent(this::sendReceipt);
                service.deleteMessage(msg);
            } catch (JAXBException | ServiceException e) {
                log.error("Failed to put message on local queue", e);
            }
        }
    }

    private void sendReceipt(ConversationResource cr) {

        if (asList(DPE_INNSYN, DPE_DATA).contains(cr.getServiceIdentifier())) {
            DpeReceiptConversationResource dpeReceipt = DpeReceiptConversationResource.of(cr);
            try {
                putMessage(dpeReceipt);
                Audit.info(format("Message [id=%s, serviceIdentifier=%s] sent to service bus",
                        dpeReceipt.getConversationId(), dpeReceipt.getServiceIdentifier()),
                        markerFrom(dpeReceipt));
            } catch (NextMoveException e) {
                log.error("Send receipt for message [id={}] failed", dpeReceipt.getConversationId(), e);
            }
        }
    }

    private ServiceBusContract createContract() {

        String sasToken;
        if (props.getOidc().isEnable()) {
            sasToken = sr.getSasToken();
        } else {
            sasToken = props.getNextbest().getServiceBus().getSasToken();
        }

        Configuration config = ServiceBusConfiguration.configureWithSASAuthentication(
                props.getNextbest().getServiceBus().getNamespace(),
                props.getNextbest().getServiceBus().getSasKeyName(),
                sasToken,
                ".servicebus.windows.net"
        );
        return ServiceBusService.create(config);
    }

    private String receiptTarget() {
        if (!isNullOrEmpty(props.getNextbest().getServiceBus().getReceiptQueue())) {
            return props.getNextbest().getServiceBus().getReceiptQueue();
        }
        if (INNSYN.fullname().equals(props.getNextbest().getServiceBus().getMode())) {
            return DATA.fullname();
        }
        return INNSYN.fullname();
    }
}
