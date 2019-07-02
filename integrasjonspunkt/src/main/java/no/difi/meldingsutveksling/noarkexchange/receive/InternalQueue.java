package no.difi.meldingsutveksling.noarkexchange.receive;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.BestEduConverter;
import no.difi.meldingsutveksling.dokumentpakking.service.SBDFactory;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.NextMoveMessageMarkers;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.noarkexchange.AppReceiptFactory;
import no.difi.meldingsutveksling.noarkexchange.IntegrajonspunktReceiveImpl;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.Session;
import java.io.ByteArrayOutputStream;
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

    private final JmsTemplate jmsTemplate;
    private final IntegrasjonspunktProperties properties;
    private final ConversationService conversationService;
    private final NextMoveSender nextMoveSender;
    private final ObjectMapper objectMapper;
    private final MessageStatusFactory messageStatusFactory;
    private final SBDFactory createSBD;
    private final NextMoveMessageService nextMoveMessageService;
    private final PutMessageRequestFactory putMessageRequestFactory;
    private final IntegrajonspunktReceiveImpl integrajonspunktReceive;
    private final NoarkClient noarkClient;
    private final DocumentConverter documentConverter;

    InternalQueue(JmsTemplate jmsTemplate,
                  IntegrasjonspunktProperties properties,
                  ConversationService conversationService,
                  NextMoveSender nextMoveSender,
                  ObjectMapper objectMapper,
                  MessageStatusFactory messageStatusFactory,
                  SBDFactory createSBD,
                  @Lazy NextMoveMessageService nextMoveMessageService,
                  PutMessageRequestFactory putMessageRequestFactory,
                  ObjectProvider<IntegrajonspunktReceiveImpl> integrajonspunktReceive,
                  @Qualifier("localNoark") ObjectProvider<NoarkClient> noarkClient,
                  DocumentConverter documentConverter) {
        this.jmsTemplate = jmsTemplate;
        this.properties = properties;
        this.conversationService = conversationService;
        this.nextMoveSender = nextMoveSender;
        this.objectMapper = objectMapper;
        this.messageStatusFactory = messageStatusFactory;
        this.createSBD = createSBD;
        this.nextMoveMessageService = nextMoveMessageService;
        this.putMessageRequestFactory = putMessageRequestFactory;
        this.integrajonspunktReceive = integrajonspunktReceive.getIfAvailable();
        this.noarkClient = noarkClient.getIfAvailable();
        this.documentConverter = documentConverter;
    }

    @JmsListener(destination = NEXTMOVE, containerFactory = "myJmsContainerFactory", concurrency = "100")
    public void nextMoveListener(byte[] message, Session session) {
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
            integrajonspunktReceive.forwardToNoarkSystem(sbd);
        } catch (Exception e) {
            Audit.warn("Failed delivering message to archive.. queue will retry", markerFrom(sbd), e);
            throw new MeldingsUtvekslingRuntimeException(e);
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
                sendBestEduErrorAppReceipt(nextMoveMessage, errorMsg);
            }
        } catch (IOException e) {
            // NOOP
        }

        // Messages attempted delivered to noark system
        try {
            StandardBusinessDocument sbd = documentConverter.unmarshallFrom(message);
            errorMsg = "Failed to forward message to noark system. Moved to DLQ.";
            Audit.error(errorMsg, markerFrom(sbd));
            conversationId = sbd.getConversationId();
            sendBestEduErrorAppReceipt(sbd);
        } catch (Exception e) {
            // NOOP
        }

        ms.setDescription(errorMsg);
        conversationService.registerStatus(conversationId, ms);
    }

    private void sendBestEduErrorAppReceipt(NextMoveOutMessage message, String errorText) {
        AppReceiptType appReceipt = AppReceiptFactory.from("ERROR", "Unknown", errorText);
        PutMessageRequestType putMessage = putMessageRequestFactory.create(message.getSbd(), BestEduConverter.appReceiptAsString(appReceipt));
        noarkClient.sendEduMelding(putMessage);
    }

    private void sendBestEduErrorAppReceipt(StandardBusinessDocument sbd) {
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
}
