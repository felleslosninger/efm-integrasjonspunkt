package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.core.EDUCoreMarker;
import no.difi.meldingsutveksling.core.EDUCoreSender;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.IntegrajonspunktReceiveImpl;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;

/**
 * The idea behind this queue is to avoid loosing messages before they are saved in Noark System.
 *
 * The way it works is that any exceptions that happens in after a message is put on the queue is re-sent to the JMS listener. If
 * the application is restarted the message is also resent.
 *
 * The JMS listener has the responsibility is to forward the message to the archive system.
 *
 */
@Component
public class InternalQueue {

    private static final String EXTERNAL = "external";
    private static final String NOARK = "noark";
    private static final String DLQ = "ActiveMQ.DLQ";

    private Logger logger = LoggerFactory.getLogger(InternalQueue.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    private IntegrajonspunktReceiveImpl integrajonspunktReceive;

    @Autowired
    private IntegrasjonspunktProperties properties;

    @Autowired
    private EDUCoreSender eduCoreSender;

    @Autowired
    private ConversationRepository conversationRepository;

    private static JAXBContext jaxbContextdomain;
    private static JAXBContext jaxbContext;

    private final DocumentConverter documentConverter = new DocumentConverter();

    @Autowired
    InternalQueue(ObjectProvider<IntegrajonspunktReceiveImpl> integrajonspunktReceive) {
        this.integrajonspunktReceive = integrajonspunktReceive.getIfAvailable();
    }

    static {
        try {
            jaxbContext = JAXBContextFactory.createContext(new Class[]{StandardBusinessDocument.class, Payload.class, Kvittering.class}, null);
            jaxbContextdomain = JAXBContextFactory.createContext(new Class[]{EduDocument.class, Payload.class, Kvittering.class}, null);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not start internal queue: Failed to create JAXBContext", e);
        }
    }

    @JmsListener(destination = NOARK, containerFactory = "myJmsContainerFactory")
    public void noarkListener(byte[] message, Session session) {
        EduDocument eduDocument = documentConverter.unmarshallFrom(message);
        try {
            forwardToNoark(eduDocument);
        } catch (Exception e) {
            Audit.warn("Failed to forward message.. queue will retry", eduDocument.createLogstashMarkers());
            throw e;
        }
    }

    @JmsListener(destination = EXTERNAL, containerFactory = "myJmsContainerFactory")
    public void externalListener(byte[] message, Session session) {
        EDUCore request = EDUCoreConverter.unmarshallFrom(message);
        try {
            eduCoreSender.sendMessage(request);
        } catch (Exception e) {
            Audit.warn("Failed to send message... queue will retry", EDUCoreMarker.markerFrom(request));
            throw e;
        }
    }

    /**
     * Log failed messages as errors
     */
    @JmsListener(destination = DLQ)
    public void dlqListener(byte[] message, Session session) {
        MessageStatus ms = MessageStatus.of(GenericReceiptStatus.FEIL.toString(), LocalDateTime.now());
        Optional<Conversation> conv = Optional.empty();
        String errorMsg = "";

        try {
            EDUCore request = EDUCoreConverter.unmarshallFrom(message);
            errorMsg = "Failed to send message. Moved to DLQ";
            Audit.error(errorMsg, EDUCoreMarker.markerFrom(request));
            conv = conversationRepository.findByConversationId(request.getId()).stream().findFirst();
        } catch (Exception e) {
        }

        try {
            EduDocument eduDocument = documentConverter.unmarshallFrom(message);
            errorMsg = "Failed to forward message. Moved to DLQ.";
            Audit.error(errorMsg, eduDocument.createLogstashMarkers());
            conv = conversationRepository.findByConversationId(eduDocument.getConversationId()).stream().findFirst();
        } catch (Exception e) {
        }

        ms.setDescription(errorMsg);
        conv.ifPresent(c -> c.addMessageStatus(ms));
        conv.ifPresent(conversationRepository::save);
    }

    /**
     * Place the input parameter on external queue. The external queue sends messages to an external recipient using transport
     * mechnism.
     *
     * @param request the input parameter from IntegrasjonspunktImpl
     */
    public void enqueueExternal(EDUCore request) {
        try {
            jmsTemplate.convertAndSend(EXTERNAL, EDUCoreConverter.marshallToBytes(request));
        } catch (Exception e) {
            Audit.error("Unable to send message", EDUCoreMarker.markerFrom(request), e);
            throw e;
        }
    }

    /**
     * Places the input parameter on the NOARK queue. The NOARK queue sends messages from external sender to NOARK server.
     *
     * @param eduDocument the eduDocument as received by IntegrasjonspunktReceiveImpl from an external source
     */
    public void enqueueNoark(EduDocument eduDocument) {
        jmsTemplate.convertAndSend(NOARK, documentConverter.marshallToBytes(eduDocument));
    }

    public void forwardToNoark(EduDocument eduDocument) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            JAXBElement<EduDocument> d = new ObjectFactory().createStandardBusinessDocument(eduDocument);

            jaxbContextdomain.createMarshaller().marshal(d, os);
            byte[] tmp = os.toByteArray();

            JAXBElement<no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument> toDocument =
                    (JAXBElement<no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument>) jaxbContext.createUnmarshaller().unmarshal(new ByteArrayInputStream(tmp));

            final StandardBusinessDocument standardBusinessDocument = toDocument.getValue();
            Audit.info("SBD extracted", markerFrom(new StandardBusinessDocumentWrapper(standardBusinessDocument)));
            sendToNoarkSystem(standardBusinessDocument);
        } catch (JAXBException e) {
            Audit.error("Failed to unserialize SBD");
            throw new MeldingsUtvekslingRuntimeException("Could not forward document to archive system", e);
        }
    }

    private void sendToNoarkSystem(StandardBusinessDocument standardBusinessDocument) {
        try {
            integrajonspunktReceive.forwardToNoarkSystem(standardBusinessDocument);
        } catch (MessageException e) {
            Audit.error("Failed delivering to archive (1)", markerFrom(new StandardBusinessDocumentWrapper
                    (standardBusinessDocument)), e);
            logger.error(markerFrom(new StandardBusinessDocumentWrapper(standardBusinessDocument)), e.getStatusMessage().getTechnicalMessage(), e);
        } catch (Exception e) {
            Audit.error("Failed delivering to archive (2)", markerFrom(new StandardBusinessDocumentWrapper
                    (standardBusinessDocument)), e);
            logger.error("Failed delivering to archive", e);
            throw e;
        }
    }

    public void setIntegrajonspunktReceiveImpl(IntegrajonspunktReceiveImpl integrajonspunktReceiveImpl) {
        this.integrajonspunktReceive = integrajonspunktReceiveImpl;
    }
}
