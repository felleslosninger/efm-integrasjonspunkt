package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.IntegrajonspunktReceiveImpl;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.Session;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;

/**
 * The idea behind this queue is to avoid loosing messages before they are saved in Noark System.
 *
 * The way it works is that any exceptions that happens in after a message is put on the queue is re-sent to the JMS
 * listener. If the application is restarted the message is also resent.
 *
 * The JMS listener has the responsibility is to forward the message to the archive system.
 *
 */
@Component
public class InternalQueue {
    private static int attempts = 0;
    Logger logger = LoggerFactory.getLogger(InternalQueue.class);

    @Autowired
    JmsTemplate jmsTemplate;
    private static final String DESTINATION = "noark-destination";

    @Autowired
    private IntegrajonspunktReceiveImpl integrajonspunktReceive;


    private static JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class, Document.class, Payload.class, Kvittering.class);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    @JmsListener(destination = DESTINATION, containerFactory = "myJmsContainerFactory")
    public void receiveMessage(byte[] message, Session session) {
        Document document = new DocumentConverter().unmarshallFrom(message);
        forwardToNoark(document);
    }

    public void put(String document) {
        jmsTemplate.convertAndSend(DESTINATION, document);
    }

    public void put(Document document) {
        jmsTemplate.convertAndSend(DESTINATION, new DocumentConverter().marshallToBytes(document));
    }

    private void forwardToNoark(Document document) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            JAXBElement<Document> d = new ObjectFactory().createStandardBusinessDocument(document);

            jaxbContext.createMarshaller().marshal(d, os);
            byte[] tmp = os.toByteArray();

            JAXBElement<no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument> toDocument
                    = (JAXBElement<no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument>)
                    jaxbContext.createUnmarshaller().unmarshal(new ByteArrayInputStream(tmp));

            final StandardBusinessDocument standardBusinessDocument = toDocument.getValue();
            Audit.info("Successfully extracted standard business document. Forwarding document to NOARK system...", markerFrom(new StandardBusinessDocumentWrapper(standardBusinessDocument)));
            try {
                integrajonspunktReceive.forwardToNoarkSystem(standardBusinessDocument);
            } catch (MessageException e) {
                Audit.error("Could not forward document to NOARK system...", markerFrom(new StandardBusinessDocumentWrapper(standardBusinessDocument)));
                logger.error(markerFrom(new StandardBusinessDocumentWrapper(standardBusinessDocument)), e.getStatusMessage().getTechnicalMessage(), e);
            }
        } catch (JAXBException e) {
            Audit.error("Could not forward document to NOARK system... due to a technical error");
            throw new MeldingsUtvekslingRuntimeException("Could not forward document to archive system", e);
        }
    }
}
