package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Lists;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.services.servicebus.ServiceBusConfiguration;
import com.microsoft.windowsazure.services.servicebus.ServiceBusContract;
import com.microsoft.windowsazure.services.servicebus.ServiceBusService;
import com.microsoft.windowsazure.services.servicebus.models.BrokeredMessage;
import com.microsoft.windowsazure.services.servicebus.models.ReceiveMessageOptions;
import com.microsoft.windowsazure.services.servicebus.models.ReceiveMode;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.MessageContext;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentFactory;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.*;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

@Component
public class NextBestServiceBus {

    private static final Logger log = LoggerFactory.getLogger(NextBestServiceBus.class);

    private static final String NEXTBEST_NS = "nextbest";
    private static final String NEXTBEST_QUEUE = "nextbestqueue";

    private ServiceBusContract service;
    private StandardBusinessDocumentFactory sbdf;
    private MessageSender messageSender;
    private JAXBContext jaxbContext;

    @Autowired
    public NextBestServiceBus(IntegrasjonspunktProperties props,
                              StandardBusinessDocumentFactory sbdf,
                              MessageSender messageSender) throws JAXBException {

        Configuration config = ServiceBusConfiguration.configureWithSASAuthentication(
                NEXTBEST_NS,
                "RootManageSharedAccessKey",
                props.getNextbest().getSasToken(),
                ".servicebus.windows.net"
        );

        this.jaxbContext = JAXBContext.newInstance(EduDocument.class, Payload.class);

        this.service = ServiceBusService.create(config);
        this.sbdf = sbdf;
        this.messageSender = messageSender;
    }

    public void putMessage(OutgoingConversationResource resource) throws NextBestException {

        BrokeredMessage msg;
        try {
            MessageContext context = messageSender.createMessageContext(resource);
            EduDocument eduDocument = sbdf.create(resource, context);

            Marshaller marshaller = jaxbContext.createMarshaller();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ObjectFactory of = new ObjectFactory();
            JAXBElement<EduDocument> sbd = of.createStandardBusinessDocument(eduDocument);
            marshaller.marshal(sbd, os);

            msg = new BrokeredMessage(os.toByteArray());

            service.sendMessage(NEXTBEST_QUEUE, msg);
        } catch (ServiceException | MessageException | JAXBException e) {
            log.error("Could not send conversation resource", e);
            throw new NextBestException(e);
        }

    }

    public List<EduDocument> getAllMessages() throws NextBestException {

        ReceiveMessageOptions opts = ReceiveMessageOptions.DEFAULT;
        opts.setReceiveMode(ReceiveMode.PEEK_LOCK);

        ArrayList<EduDocument> messages = Lists.newArrayList();
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            for (;;) {
                BrokeredMessage msg = service.receiveQueueMessage(NEXTBEST_QUEUE, opts).getValue();

                if (msg == null || isNullOrEmpty(msg.getMessageId())) break;
                log.info("Received message from queue \"{}\" with id {}", NEXTBEST_QUEUE, msg.getMessageId());

                EduDocument eduDocument = ((JAXBElement<EduDocument>) unmarshaller.unmarshal(msg.getBody())).getValue();
                messages.add(eduDocument);
                service.deleteMessage(msg);
            }
        } catch (ServiceException | JAXBException e) {
            log.error("Failed to fetch new message(s)", e);
            throw new NextBestException(e);
        }

        return messages;
    }

}
