package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.*;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Payload;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.nextmove.NextMoveSender;
import no.difi.meldingsutveksling.nextmove.logging.ConversationResourceMarkers;
import no.difi.meldingsutveksling.noarkexchange.*;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.Session;
import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;
import static no.difi.meldingsutveksling.nextmove.logging.ConversationResourceMarkers.markerFrom;

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
    private static final String NEXTMOVE = "nextmove";
    private static final String PUTMSG = "putmessage";
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
    private ConversationService conversationService;

    @Autowired
    private NextMoveSender nextMoveSender;

    private NoarkClient noarkClient;

    private EDUCoreFactory eduCoreFactory;

    private static JAXBContext jaxbContext;
    private static JAXBContext jaxbContextNextmove;

    private final DocumentConverter documentConverter = new DocumentConverter();

    @Autowired
    InternalQueue(ObjectProvider<IntegrajonspunktReceiveImpl> integrajonspunktReceive,
                  @Qualifier("localNoark") ObjectProvider<NoarkClient> noarkClient,
                  EDUCoreFactory eduCoreFactory) {
        this.integrajonspunktReceive = integrajonspunktReceive.getIfAvailable();
        this.noarkClient = noarkClient.getIfAvailable();
        this.eduCoreFactory = eduCoreFactory;
    }

    static {
        try {
            jaxbContext = JAXBContextFactory.createContext(new Class[]{EduDocument.class, Payload.class, Kvittering.class, PutMessageRequestType.class}, null);
            jaxbContextNextmove = JAXBContextFactory.createContext(new Class[]{ConversationResource.class}, null);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not start internal queue: Failed to create JAXBContext", e);
        }
    }

    @JmsListener(destination = NEXTMOVE, containerFactory = "myJmsContainerFactory", concurrency = "100")
    public void nextmoveListener(byte[] message, Session session) {
        ConversationResource cr = unmarshalNextMoveMessage(message);
        try {
            nextMoveSender.send(cr);
        } catch (Exception e) {
            Audit.warn("Failed to send message... queue will retry", ConversationResourceMarkers.markerFrom(cr), e);
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    @JmsListener(destination = NOARK, containerFactory = "myJmsContainerFactory")
    public void noarkListener(byte[] message, Session session) {
        EduDocument eduDocument = documentConverter.unmarshallFrom(message);
        try {
            forwardToNoark(eduDocument);
        } catch (Exception e) {
            Audit.warn("Failed to forward message.. queue will retry", eduDocument.createLogstashMarkers(), e);
            throw e;
        }
    }

    @JmsListener(destination = PUTMSG, containerFactory = "myJmsContainerFactory")
    public void putMessageListener(byte[] message, Session session) {
        PutMessageRequestType putMessage = unmarshalPutMessage(message);
        try {
            noarkClient.sendEduMelding(putMessage);
        } catch (Exception e) {
            Audit.warn("Failed to forward message.. queue will retry", PutMessageMarker.markerFrom(new PutMessageRequestWrapper(putMessage)), e);
            throw e;
        }
    }

    @JmsListener(destination = EXTERNAL, containerFactory = "myJmsContainerFactory")
    public void externalListener(byte[] message, Session session) {
        EDUCore request = EDUCoreConverter.unmarshallFrom(message);
        try {
            eduCoreSender.sendMessage(request);
        } catch (Exception e) {
            Audit.warn("Failed to send message... queue will retry", EDUCoreMarker.markerFrom(request), e);
            throw e;
        }
    }

    /**
     * Log failed messages as errors
     */
    @SuppressWarnings("squid:S1166")
    @JmsListener(destination = DLQ)
    public void dlqListener(byte[] message, Session session) {
        MessageStatus ms = MessageStatus.of(GenericReceiptStatus.FEIL);
        String conversationId = "";
        String errorMsg = "";

        try {
            EDUCore request = EDUCoreConverter.unmarshallFrom(message);
            errorMsg = "Failed to send message. Moved to DLQ";
            Audit.error(errorMsg, EDUCoreMarker.markerFrom(request));
            conversationId = request.getId();
            if (noarkClient != null) {
                sendErrorAppReceipt(request);
            }
            if (properties.getFeature().isDumpDlqMessages()) {
                writeMessageToDisk(request);
            }
        } catch (Exception e) {
        }

        try {
            EduDocument eduDocument = documentConverter.unmarshallFrom(message);
            errorMsg = "Failed to forward message. Moved to DLQ.";
            Audit.error(errorMsg, eduDocument.createLogstashMarkers());
            conversationId = eduDocument.getConversationId();
            sendErrorAppReceipt(eduDocument);
        } catch (Exception e) {
        }

        try {
            ConversationResource cr = unmarshalNextMoveMessage(message);
            errorMsg = "Failed to send message. Moved to DLQ";
            Audit.error(errorMsg, ConversationResourceMarkers.markerFrom(cr));
            conversationId = cr.getConversationId();
        } catch (Exception e) {
        }

        ms.setDescription(errorMsg);
        conversationService.registerStatus(conversationId, ms);
    }

    private void writeMessageToDisk(EDUCore request) {
        PutMessageRequestType putMessage = EDUCoreFactory.createPutMessageFromCore(request);
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            marshaller.marshal(new JAXBElement<>(new QName("uri", "local"), PutMessageRequestType.class, putMessage), bos);
            FileOutputStream fos = new FileOutputStream(new File("failed_messages/" + request.getId() + "_failed.xml"));
            bos.writeTo(fos);
        } catch (JAXBException | IOException e) {
            logger.error("Failed writing message to disk", e);
        }
    }

    private ConversationResource unmarshalNextMoveMessage(byte[] message) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(message);
            StreamSource ss = new StreamSource(bis);
            Unmarshaller unmarshaller = jaxbContextNextmove.createUnmarshaller();
            return unmarshaller.unmarshal(ss, ConversationResource.class).getValue();
        } catch (JAXBException e) {
            logger.error("Could not unmarshal nextmove message", e);
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    private PutMessageRequestType unmarshalPutMessage(byte[] message) {
        ByteArrayInputStream bis = new ByteArrayInputStream(message);
        StreamSource ss = new StreamSource(bis);
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return unmarshaller.unmarshal(ss, PutMessageRequestType.class).getValue();
        } catch (JAXBException e) {
            logger.error("Could not unmarshal putmessage", e);
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    private void sendErrorAppReceipt(EDUCore request) {
        AppReceiptType receipt = new AppReceiptType();
        receipt.setType("ERROR");
        StatusMessageType statusMessageType = new StatusMessageType();
        statusMessageType.setCode("ID");
        statusMessageType.setText(String.format("Feilet ved sending til %s", request.getServiceIdentifier()));
        receipt.getMessage().add(statusMessageType);

        Object oldPayload = request.getPayload();
        request.swapSenderAndReceiver();
        request.setMessageType(EDUCore.MessageType.APPRECEIPT);
        request.setPayload(EDUCoreConverter.appReceiptAsString(receipt));
        PutMessageRequestType putMessage = EDUCoreFactory.createPutMessageFromCore(request);
        noarkClient.sendEduMelding(putMessage);
        request.setPayload(oldPayload);
        request.setMessageType(EDUCore.MessageType.EDU);
        request.swapSenderAndReceiver();
    }

    private void sendErrorAppReceipt(EduDocument eduDocument) {
        AppReceiptType receipt = new AppReceiptType();
        receipt.setType("ERROR");
        StatusMessageType statusMessageType = new StatusMessageType();
        statusMessageType.setCode("ID");
        statusMessageType.setText(String.format("Feilet under mottak hos %s", eduDocument.getReceiverOrgNumber()));
        receipt.getMessage().add(statusMessageType);

        EDUCore eduCore = eduCoreFactory.create(receipt,
                eduDocument.getConversationId(),
                eduDocument.getReceiverOrgNumber(),
                eduDocument.getSenderOrgNumber());
        enqueueExternal(eduCore);
    }

    public void enqueueNextmove(ConversationResource cr) {
        try {
            Marshaller marshaller = jaxbContextNextmove.createMarshaller();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            marshaller.marshal(new JAXBElement<>(new QName("uri", "local"), ConversationResource.class, cr), bos);
            jmsTemplate.convertAndSend(NEXTMOVE, bos.toByteArray());
        } catch (JAXBException e) {
            Audit.error("Unable to queue message", markerFrom(cr), e);
            throw new MeldingsUtvekslingRuntimeException(e);
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

    public void enqueuePutMessage(PutMessageRequestType putMessage) {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            marshaller.marshal(new JAXBElement<>(new QName("uri", "local"), PutMessageRequestType.class, putMessage), bos);
            jmsTemplate.convertAndSend(PUTMSG, bos.toByteArray());
        } catch (JAXBException e) {
            Audit.error("Unable to queue putmessage", PutMessageMarker.markerFrom(new PutMessageRequestWrapper(putMessage)), e);
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public void forwardToNoark(EduDocument eduDocument) {
        try {
            sendToNoarkSystem(eduDocument);
        } catch (Exception e) {
            Audit.error("Failed to unserialize SBD");
            throw new MeldingsUtvekslingRuntimeException("Could not forward document to archive system", e);
        }
    }

    private void sendToNoarkSystem(EduDocument standardBusinessDocument) {
        try {
            integrajonspunktReceive.forwardToNoarkSystem(standardBusinessDocument);
        } catch (Exception e) {
            Audit.error("Failed delivering to archive", markerFrom(standardBusinessDocument), e);
            if (e instanceof MessageException) {
                logger.error(markerFrom(standardBusinessDocument), ((MessageException)e).getStatusMessage().getTechnicalMessage(), e);
            }
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public void setIntegrajonspunktReceiveImpl(IntegrajonspunktReceiveImpl integrajonspunktReceiveImpl) {
        this.integrajonspunktReceive = integrajonspunktReceiveImpl;
    }
}
