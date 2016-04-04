package no.difi.meldingsutveksling.noarkexchange.receive;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.kvittering.KvitteringFactory;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.MessageMarkerFactory;
import no.difi.meldingsutveksling.noarkexchange.*;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
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
    private static final String EXTERNAL = "external";
    private static int attempts = 0;
    Logger logger = LoggerFactory.getLogger(InternalQueue.class);

    @Autowired
    JmsTemplate jmsTemplate;
    private static final String NOARK = "noark";

    @Autowired
    private IntegrajonspunktReceiveImpl integrajonspunktReceive;

    @Autowired
    private IntegrasjonspunktImpl integrasjonspunktSend;

    @Autowired
    private IntegrasjonspunktConfiguration configuration;

    @Autowired
    IntegrasjonspunktNokkel keyInfo;

    @Autowired
    TransportFactory transportFactory;

    @Autowired
    IntegrasjonspunktConfiguration config;

    private final DocumentConverter documentConverter = new DocumentConverter();

    private static JAXBContext jaxbContextdomain;
    private static JAXBContext jaxbContext;

    private PutMessageRequestConverter putMessageRequestConverter = new PutMessageRequestConverter();

    static {
        try {
            jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class, Payload.class, Kvittering.class);
            jaxbContextdomain = JAXBContext.newInstance(Document.class, Payload.class, Kvittering.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not start internal queue: Failed to create JAXBContext", e);
        }
    }


    @JmsListener(destination = NOARK, containerFactory = "myJmsContainerFactory")
    public void noarkListener(byte[] message, Session session) {
        MDC.put(IntegrasjonspunktConfiguration.KEY_ORGANISATION_NUMBER, configuration.getOrganisationNumber());
        Document document = documentConverter.unmarshallFrom(message);
        forwardToNoark(document);
    }

    @JmsListener(destination = EXTERNAL, containerFactory = "myJmsContainerFactory")
    public void externalListener(byte[] message, Session session) {
        MDC.put(IntegrasjonspunktConfiguration.KEY_ORGANISATION_NUMBER, configuration.getOrganisationNumber());
        PutMessageRequestType requestType = putMessageRequestConverter.unmarshallFrom(message);
        try {
            integrasjonspunktSend.sendMessage(requestType);
        } catch (Exception e) {
            Audit.error("Failed to send message... queue will retry", markerFrom(new PutMessageRequestWrapper(requestType)));
            throw e;
        }
    }

    /**
     * Place the input parameter on external queue. The external queue sends messages to an external recipient
     * using transport mechnism.
     * @param request the input parameter from IntegrasjonspunktImpl
     */
    public void enqueueExternal(PutMessageRequestType request) {
        try {
            jmsTemplate.convertAndSend(EXTERNAL, putMessageRequestConverter.marshallToBytes(request));
        } catch (Exception e) {
            Audit.error("Unable to send message", markerFrom(new PutMessageRequestWrapper(request)));
            throw e;
        }
    }

    /**
     * Places the input parameter on the NOARK queue. The NOARK queue sends messages from external sender to
     * NOARK server.
     * @param document the document as received by IntegrasjonspunktReceiveImpl from an external source
     */
    public void enqueueNoark(Document document) {
        sendReceipt(document.getMessageInfo());
        Audit.info("Delivery receipt sent", markerFrom(document.getMessageInfo()));
        jmsTemplate.convertAndSend(NOARK,  documentConverter.marshallToBytes(document));
    }

    private void forwardToNoark(Document document) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            JAXBElement<Document> d = new ObjectFactory().createStandardBusinessDocument(document);

            jaxbContextdomain.createMarshaller().marshal(d, os);
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
            } catch (Exception e) {
                Audit.error("Unable to send document to NOARK system", markerFrom(new StandardBusinessDocumentWrapper(standardBusinessDocument)));
                throw e;
            }
        } catch (JAXBException e) {
            Audit.error("Could not forward document to NOARK system... due to a technical error");
            throw new MeldingsUtvekslingRuntimeException("Could not forward document to archive system", e);
        }
    }

    private void sendReceipt(MessageInfo messageInfo) {
        Document doc = KvitteringFactory.createLeveringsKvittering(messageInfo, keyInfo.getKeyPair());
        Transport t = transportFactory.createTransport(doc);
        t.send(config.getConfiguration(), doc);
    }
}
