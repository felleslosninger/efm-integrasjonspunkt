package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreMarker;
import no.difi.meldingsutveksling.core.EDUCoreSender;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.MoveLogMarkers;
import no.difi.meldingsutveksling.noarkexchange.IntegrajonspunktReceiveImpl;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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

    private static JAXBContext jaxbContextdomain;
    private static JAXBContext jaxbContext;

    private final DocumentConverter documentConverter = new DocumentConverter();
    private final EDUCoreConverter eduCoreConverter = new EDUCoreConverter();

    @Autowired
    InternalQueue(ObjectProvider<IntegrajonspunktReceiveImpl> integrajonspunktReceive) {
        this.integrajonspunktReceive = integrajonspunktReceive.getIfAvailable();
    }

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
        MDC.put(MoveLogMarkers.KEY_ORGANISATION_NUMBER, properties.getOrg().getNumber());
        EduDocument eduDocument = documentConverter.unmarshallFrom(message);
        try {
            forwardToNoark(eduDocument);
        } catch (Exception e) {
            Audit.warn("Failed to forward message.. queue will retry", eduDocument.createLogstashMarkers());
        }
    }

    @JmsListener(destination = EXTERNAL, containerFactory = "myJmsContainerFactory")
    public void externalListener(byte[] message, Session session) {
        MDC.put(MoveLogMarkers.KEY_ORGANISATION_NUMBER, properties.getOrg().getNumber());
        EDUCore request = eduCoreConverter.unmarshallFrom(message);
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

        try {
            EDUCore request = eduCoreConverter.unmarshallFrom(message);
            Audit.error("Failed to send message. Moved to DLQ", EDUCoreMarker.markerFrom(request));
        } catch (Exception e) {
        }

        try {
            EduDocument eduDocument = documentConverter.unmarshallFrom(message);
            Audit.error("Failed to forward message. Moved to DLQ.", eduDocument.createLogstashMarkers());
        } catch (Exception e) {
        }

    }

    /**
     * Place the input parameter on external queue. The external queue sends messages to an external recipient using transport
     * mechnism.
     *
     * @param request the input parameter from IntegrasjonspunktImpl
     */
    public void enqueueExternal(EDUCore request) {
        try {
            jmsTemplate.convertAndSend(EXTERNAL, eduCoreConverter.marshallToBytes(request));
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
