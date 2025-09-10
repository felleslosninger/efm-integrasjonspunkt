package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.NextMoveMessageMarkers;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;

@Component
public class DeadLetterQueueHandler {

    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final MessageStatusFactory messageStatusFactory;
    private final DocumentConverter documentConverter;
    private final NextMoveMessageService messageService;

    public DeadLetterQueueHandler(
            ConversationService conversationService,
            ObjectMapper objectMapper,
            MessageStatusFactory messageStatusFactory,
            DocumentConverter documentConverter,
            @Lazy NextMoveMessageService messageService) {
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
        this.messageStatusFactory = messageStatusFactory;
        this.documentConverter = documentConverter;
        this.messageService = messageService;
    }

    void handleNextMoveMessage(NextMoveOutMessage message, String errorMsg) {
        MessageStatus ms = messageStatusFactory.getMessageStatus(ReceiptStatus.FEIL);
        ms.setDescription("Failed to deliver message - check raw receipt");
        ms.setRawReceipt(errorMsg);
        conversationService.registerStatus(message.getMessageId(), ms);
        messageService.deleteMessage(message.getMessageId());
    }

    void handleDlqMessage(byte[] message) {
        String messageId = "";
        String errorMsg = "";

        // Outgoing NextMove messages
        try {
            NextMoveOutMessage nextMoveMessage = objectMapper.readValue(message, NextMoveOutMessage.class);
            errorMsg = "Request to receiver '%s' failed delivery over %s - Moved to DLQ".formatted(
                    nextMoveMessage.getReceiverIdentifier(), nextMoveMessage.getServiceIdentifier());
            messageId = nextMoveMessage.getMessageId();
            Audit.error(errorMsg, NextMoveMessageMarkers.markerFrom(nextMoveMessage));
            messageService.deleteMessage(messageId);
        } catch (IOException e) {
            // NOOP
        }

        // Messages attempted delivered to noark system
        try {
            StandardBusinessDocument sbd = documentConverter.unmarshallFrom(message);
            errorMsg = "Failed to forward message to noark system. Moved to DLQ.";
            Audit.error(errorMsg, markerFrom(sbd));
            messageId = sbd.getMessageId();
        } catch (Exception e) {
            // NOOP
        }

        conversationService.registerStatus(messageId, ReceiptStatus.FEIL, errorMsg);
    }
}
