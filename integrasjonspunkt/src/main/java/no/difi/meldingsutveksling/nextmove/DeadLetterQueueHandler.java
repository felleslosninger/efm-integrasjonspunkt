package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.NextMoveMessageMarkers;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.noarkexchange.BestEduAppReceiptService;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;

@Component
public class DeadLetterQueueHandler {

    private final IntegrasjonspunktProperties properties;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final MessageStatusFactory messageStatusFactory;
    private final NoarkClient noarkClient;
    private final DocumentConverter documentConverter;
    private final BestEduAppReceiptService bestEduAppReceiptService;
    private final NextMoveMessageService messageService;

    public DeadLetterQueueHandler(
            IntegrasjonspunktProperties properties,
            ConversationService conversationService,
            ObjectMapper objectMapper,
            MessageStatusFactory messageStatusFactory,
            @Qualifier("localNoark") ObjectProvider<NoarkClient> noarkClient,
            DocumentConverter documentConverter,
            BestEduAppReceiptService bestEduAppReceiptService,
            @Lazy NextMoveMessageService messageService) {
        this.properties = properties;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
        this.messageStatusFactory = messageStatusFactory;
        this.noarkClient = noarkClient.getIfAvailable();
        this.documentConverter = documentConverter;
        this.bestEduAppReceiptService = bestEduAppReceiptService;
        this.messageService = messageService;
    }

    void handleNextMoveMessage(NextMoveOutMessage message, String errorMsg) {
        MessageStatus ms = messageStatusFactory.getMessageStatus(ReceiptStatus.FEIL);
        ms.setDescription("Failed to deliver message - check raw receipt");
        ms.setRawReceipt(errorMsg);
        conversationService.registerStatus(message.getMessageId(), ms);
        if (!isNullOrEmpty(properties.getNoarkSystem().getType()) && noarkClient != null) {
            bestEduAppReceiptService.sendBestEduErrorAppReceipt(message, errorMsg);
        }
        messageService.deleteMessage(message.getMessageId());
    }

    void handleDlqMessage(byte[] message) {
        String messageId = "";
        String errorMsg = "";

        // Outgoing NextMove messages
        try {
            NextMoveOutMessage nextMoveMessage = objectMapper.readValue(message, NextMoveOutMessage.class);
            errorMsg = String.format("Request to receiver '%s' failed delivery over %s - Moved to DLQ",
                    nextMoveMessage.getReceiverIdentifier(), nextMoveMessage.getServiceIdentifier());
            messageId = nextMoveMessage.getMessageId();
            Audit.error(errorMsg, NextMoveMessageMarkers.markerFrom(nextMoveMessage));
            if (!isNullOrEmpty(properties.getNoarkSystem().getType()) && noarkClient != null) {
                bestEduAppReceiptService.sendBestEduErrorAppReceipt(nextMoveMessage, errorMsg);
            }
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
            bestEduAppReceiptService.sendBestEduErrorAppReceipt(sbd);
        } catch (Exception e) {
            // NOOP
        }

        conversationService.registerStatus(messageId, ReceiptStatus.FEIL, errorMsg);
    }
}
