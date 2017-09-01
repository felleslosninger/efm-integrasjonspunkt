package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Lists;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.services.servicebus.ServiceBusConfiguration;
import com.microsoft.windowsazure.services.servicebus.ServiceBusContract;
import com.microsoft.windowsazure.services.servicebus.ServiceBusService;
import com.microsoft.windowsazure.services.servicebus.models.*;
import no.difi.meldingsutveksling.ServiceIdentifier;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

@Component
public class NextMoveServiceBus {

    private static final Logger log = LoggerFactory.getLogger(NextMoveServiceBus.class);

    private static final String NEXTMOVE_QUEUE_PREFIX = "nextbestqueue";

    private IntegrasjonspunktProperties props;
    private ServiceRegistryLookup sr;
    private StandardBusinessDocumentFactory sbdf;
    private MessageSender messageSender;
    private JAXBContext jaxbContext;
    private String queuePath;

    @Autowired
    public NextMoveServiceBus(IntegrasjonspunktProperties props,
                              StandardBusinessDocumentFactory sbdf,
                              ServiceRegistryLookup sr,
                              MessageSender messageSender) throws JAXBException {

        this.props = props;
        this.sbdf = sbdf;
        this.sr = sr;
        this.messageSender = messageSender;


        this.jaxbContext = JAXBContext.newInstance(EduDocument.class, Payload.class, ConversationResource.class);

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
            if (ServiceIdentifier.DPE_INNSYN == resource.getServiceIdentifier()) {
                queue = queue + ServiceBusQueueMode.INNSYN.fullname();
            } else {
                queue = queue + ServiceBusQueueMode.DATA.fullname();
            }
            service.sendMessage(queue, msg);
        } catch (ServiceException | MessageException | JAXBException | IOException e) {
            log.error("Could not send conversation resource", e);
            throw new NextMoveException(e);
        }

    }

    public List<EduDocument> getAllMessages() throws NextMoveException {

        ReceiveMessageOptions opts = ReceiveMessageOptions.DEFAULT;
        opts.setReceiveMode(ReceiveMode.PEEK_LOCK);

        ServiceBusContract service = createContract();
        ArrayList<EduDocument> messages = Lists.newArrayList();
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            BrokeredMessage msg;
            while ((msg = service.receiveQueueMessage(queuePath, opts).getValue()) != null) {

                if (isNullOrEmpty(msg.getMessageId())) {
                    break;
                }
                Audit.info(format("Received message on queue=%s with id=%s", queuePath, msg.getMessageId()));

                EduDocument eduDocument = ((JAXBElement<EduDocument>) unmarshaller.unmarshal(msg.getBody())).getValue();
                messages.add(eduDocument);
                service.deleteMessage(msg);
            }
        } catch (ServiceException | JAXBException e) {
            log.error("Failed to fetch new message(s)", e);
            throw new NextMoveException(e);
        }

        return messages;
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
}
