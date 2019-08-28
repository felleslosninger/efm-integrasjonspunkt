package no.difi.meldingsutveksling.noarkexchange.receive;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.NextMoveMessageMarkers;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final BestEduErrorAppReceiptService bestEduErrorAppReceiptService;

    public DeadLetterQueueHandler(
            IntegrasjonspunktProperties properties,
            ConversationService conversationService,
            ObjectMapper objectMapper,
            MessageStatusFactory messageStatusFactory,
            @Qualifier("localNoark") ObjectProvider<NoarkClient> noarkClient,
            DocumentConverter documentConverter,
            BestEduErrorAppReceiptService bestEduErrorAppReceiptService) {
        this.properties = properties;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
        this.messageStatusFactory = messageStatusFactory;
        this.noarkClient = noarkClient.getIfAvailable();
        this.documentConverter = documentConverter;
        this.bestEduErrorAppReceiptService = bestEduErrorAppReceiptService;
    }

    void handleDlqMessage(byte[] message) {
        MessageStatus ms = messageStatusFactory.getMessageStatus(ReceiptStatus.FEIL);
        String messageId = "";
        String errorMsg = "";

        // Outgoing NextMove messages
        try {
            NextMoveOutMessage nextMoveMessage = objectMapper.readValue(message, NextMoveOutMessage.class);
            errorMsg = String.format("Request to receiver '%s' failed delivery over %s - Moved to DLQ",
                    nextMoveMessage.getReceiverIdentifier(), nextMoveMessage.getServiceIdentifier());
            messageId = nextMoveMessage.getMessageId();
            Audit.error(errorMsg, NextMoveMessageMarkers.markerFrom(nextMoveMessage));
            if (!isNullOrEmpty(properties.getNoarkSystem().getEndpointURL()) && noarkClient != null) {
                bestEduErrorAppReceiptService.sendBestEduErrorAppReceipt(nextMoveMessage, errorMsg);
            }
        } catch (IOException e) {
            // NOOP
        }

        // Messages attempted delivered to noark system
        try {
            StandardBusinessDocument sbd = documentConverter.unmarshallFrom(message);
            errorMsg = "Failed to forward message to noark system. Moved to DLQ.";
            Audit.error(errorMsg, markerFrom(sbd));
            messageId = sbd.getDocumentId();
            bestEduErrorAppReceiptService.sendBestEduErrorAppReceipt(sbd);
        } catch (Exception e) {
            // NOOP
        }

        ms.setDescription(errorMsg);
        conversationService.registerStatus(messageId, ms);
    }
}
