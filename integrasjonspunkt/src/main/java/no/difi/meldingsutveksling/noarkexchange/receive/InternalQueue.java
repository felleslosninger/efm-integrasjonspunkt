package no.difi.meldingsutveksling.noarkexchange.receive;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreFactory;
import no.difi.meldingsutveksling.dokumentpakking.service.SBDFactory;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.Payload;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.noarkexchange.AppReceiptFactory;
import no.difi.meldingsutveksling.noarkexchange.IntegrajonspunktReceiveImpl;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.Session;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;

/**
 * The idea behind this queue is to avoid loosing messages before they are saved in Noark System.
 * <p>
 * The way it works is that any exceptions that happens in after a message is put on the queue is re-sent to the JMS listener. If
 * the application is restarted the message is also resent.
 * <p>
 * The JMS listener has the responsibility is to forward the message to the archive system.
 */
@Component
@Slf4j
public class InternalQueue {

    private static final String NOARK = "noark";
    private static final String NEXTMOVE = "nextmove";
    private static final String DLQ = "ActiveMQ.DLQ";

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private IntegrasjonspunktProperties properties;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private NextMoveSender nextMoveSender;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired // Avoid circular depencency
    private MessageStatusFactory messageStatusFactory;

    @Autowired
    private SBDFactory createSBD;

    @Autowired
    private NextMoveMessageService nextMoveMessageService;

    private final IntegrajonspunktReceiveImpl integrajonspunktReceive;
    private final NoarkClient noarkClient;
    private final EDUCoreFactory eduCoreFactory;

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
            jaxbContext = JAXBContextFactory.createContext(new Class[]{StandardBusinessDocument.class, Payload.class, Kvittering.class, PutMessageRequestType.class}, null);
            jaxbContextNextmove = JAXBContextFactory.createContext(new Class[]{ConversationResource.class}, null);
        } catch (JAXBException e) {
            throw new NextMoveRuntimeException("Could not start internal queue: Failed to create JAXBContext", e);
        }
    }

    @JmsListener(destination = NEXTMOVE, containerFactory = "myJmsContainerFactory", concurrency = "100")
    public void nextMove2Listener(byte[] message, Session session) {
        NextMoveOutMessage nextMoveMessage;
        try {
            nextMoveMessage = objectMapper.readValue(message, NextMoveOutMessage.class);
        } catch (IOException e) {
            throw new NextMoveRuntimeException("Unable to unmarshall NextMove message from queue", e);
        }
        try {
            nextMoveSender.send(nextMoveMessage);
        } catch (NextMoveException e) {
            throw new NextMoveRuntimeException("Unable to send NextMove message", e);
        }
    }

    @JmsListener(destination = NOARK, containerFactory = "myJmsContainerFactory")
    public void noarkListener(byte[] message, Session session) {
        StandardBusinessDocument sbd = documentConverter.unmarshallFrom(message);
        try {
            forwardToNoark(sbd);
        } catch (Exception e) {
            Audit.warn("Failed to forward message.. queue will retry", sbd.createLogstashMarkers(), e);
            throw e;
        }
    }

    /**
     * Log failed messages as errors
     */
    @SuppressWarnings("squid:S1166")
    @JmsListener(destination = DLQ)
    public void dlqListener(byte[] message, Session session) {
        MessageStatus ms = messageStatusFactory.getMessageStatus(ReceiptStatus.FEIL);
        String conversationId = "";
        String errorMsg = "";

        // Outgoing NextMove messages
        try {
            NextMoveOutMessage nextMoveMessage = objectMapper.readValue(message, NextMoveOutMessage.class);
            errorMsg = String.format("Request to receiver '%s' failed delivery over %s - Moved to DLQ",
                    nextMoveMessage.getReceiverIdentifier(), nextMoveMessage.getServiceIdentifier());
            conversationId = nextMoveMessage.getConversationId();
            Audit.error(errorMsg, NextMoveMessageMarkers.markerFrom(nextMoveMessage));
            if (properties.getNoarkSystem().isEnable() && noarkClient != null) {
                sendErrorAppReceipt(nextMoveMessage, errorMsg);
            }
        } catch (IOException e) {
        }

        // Messages attempted delivered to noark system
        try {
            StandardBusinessDocument sbd = documentConverter.unmarshallFrom(message);
            errorMsg = "Failed to forward message to noark system. Moved to DLQ.";
            Audit.error(errorMsg, sbd.createLogstashMarkers());
            conversationId = sbd.getConversationId();
            sendErrorAppReceipt(sbd);
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
            log.error("Failed writing message to disk", e);
        }
    }

    private void sendErrorAppReceipt(NextMoveOutMessage message, String errorText) {
        AppReceiptType appReceipt = AppReceiptFactory.from("ERROR", "Unknown", errorText);

        EDUCore eduCore = eduCoreFactory.create(appReceipt,
                message.getConversationId(),
                message.getSenderIdentifier(),
                message.getReceiverIdentifier());
        PutMessageRequestType putMessage = EDUCoreFactory.createPutMessageFromCore(eduCore);
        noarkClient.sendEduMelding(putMessage);
    }

    private void sendErrorAppReceipt(StandardBusinessDocument sbd) {
        String errorText = String.format("Feilet under mottak hos %s - ble ikke avlevert sakarkivsystem", sbd.getReceiverIdentifier());
        ArkivmeldingKvitteringMessage kvittering = new ArkivmeldingKvitteringMessage()
                .setReceiptType("ERROR")
                .addMessage(new KvitteringStatusMessage("Unknown", errorText));

        StandardBusinessDocument receiptSbd = createSBD.createNextMoveSBD(Organisasjonsnummer.from(sbd.getReceiverIdentifier()),
                Organisasjonsnummer.from(sbd.getSenderIdentifier()),
                sbd.getConversationId(),
                properties.getArkivmelding().getReceiptProcess(),
                DocumentType.ARKIVMELDING_KVITTERING,
                kvittering);

        NextMoveOutMessage message = nextMoveMessageService.createMessage(receiptSbd);
        nextMoveMessageService.sendMessage(message);
    }

    public void enqueueNextMove(NextMoveOutMessage msg) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            objectMapper.writeValue(bos, msg);
            jmsTemplate.convertAndSend(NEXTMOVE, bos.toByteArray());
        } catch (IOException e) {
            throw new NextMoveRuntimeException(String.format("Unable to marshall NextMove message with id=%s", msg.getConversationId()), e);
        }
    }

    /**
     * Places the input parameter on the NOARK queue. The NOARK queue sends messages from external sender to NOARK server.
     *
     * @param sbd the sbd as received by IntegrasjonspunktReceiveImpl from an external source
     */
    public void enqueueNoark(StandardBusinessDocument sbd) {
        jmsTemplate.convertAndSend(NOARK, documentConverter.marshallToBytes(sbd));
    }

    public void forwardToNoark(StandardBusinessDocument sbd) {
        try {
            sendToNoarkSystem(sbd);
        } catch (Exception e) {
            Audit.error("Failed to unserialize SBD");
            throw new MeldingsUtvekslingRuntimeException("Could not forward document to archive system", e);
        }
    }

    private void sendToNoarkSystem(StandardBusinessDocument sbd) {
        try {
            integrajonspunktReceive.forwardToNoarkSystem(sbd);
        } catch (Exception e) {
            Audit.error("Failed delivering to archive", markerFrom(sbd), e);
            if (e instanceof MessageException) {
                log.error(markerFrom(sbd), ((MessageException) e).getStatusMessage().getTechnicalMessage(), e);
            }
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }
}
