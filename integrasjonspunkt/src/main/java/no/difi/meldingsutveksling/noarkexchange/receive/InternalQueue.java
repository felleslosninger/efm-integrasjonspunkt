package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.kvittering.KvitteringFactory;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.*;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
            jaxbContextdomain = JAXBContext.newInstance(EduDocument.class, Payload.class, Kvittering.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not start internal queue: Failed to create JAXBContext", e);
        }
    }


    @JmsListener(destination = NOARK, containerFactory = "myJmsContainerFactory")
    public void noarkListener(byte[] message, Session session) {
        MDC.put(IntegrasjonspunktConfiguration.KEY_ORGANISATION_NUMBER, configuration.getOrganisationNumber());
        EduDocument eduDocument = documentConverter.unmarshallFrom(message);
        forwardToNoark(eduDocument);
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
     * @param eduDocument the eduDocument as received by IntegrasjonspunktReceiveImpl from an external source
     */
    public void enqueueNoark(EduDocument eduDocument) {

        if(!isKvittering(eduDocument)) {
            sendReceipt(eduDocument.getMessageInfo());
            Audit.info("Delivery receipt sent", markerFrom(eduDocument.getMessageInfo()));
            jmsTemplate.convertAndSend(NOARK,  documentConverter.marshallToBytes(eduDocument));
        } else {
            Audit.info("Message is a receipt", markerFrom(eduDocument.getMessageInfo()));
        }
    }

    private boolean isKvittering(EduDocument eduDocument) {
        return eduDocument.getStandardBusinessDocumentHeader().getDocumentIdentification().getType().equalsIgnoreCase(StandardBusinessDocumentHeader.KVITTERING_TYPE);
    }

    private void forwardToNoark(EduDocument eduDocument) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            JAXBElement<EduDocument> d = new ObjectFactory().createStandardBusinessDocument(eduDocument);

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
        EduDocument doc = KvitteringFactory.createLeveringsKvittering(messageInfo, keyInfo.getKeyPair());
        Transport t = transportFactory.createTransport(doc);
        t.send(config.getConfiguration(), doc);
    }
}
