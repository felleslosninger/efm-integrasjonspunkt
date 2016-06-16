package no.difi.meldingsutveksling.logging;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.noarkexchange.PayloadException;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.difi.meldingsutveksling.logging.MarkerFactory.*;
import static no.difi.meldingsutveksling.noarkexchange.PayloadUtil.isAppReceipt;


/**
 * Example usage:
 * import static no.difi.meldingsutveksling.logging.ConversationIdMarkerFactory.markerFrom;
 * <p/>
 * ...
 * <p/>
 * log.error(markerFrom(message), "putting message");
 */
public class MessageMarkerFactory {
    private static final String DOCUMENT_ID = "document_id";
    private static final String ALTINN_RECEIPT_ID = "altinn-receipt-id";
    private static final String PAYLOAD_SIZE = "payload-size";


    private static final Logger logger = LoggerFactory.getLogger(MessageMarkerFactory.class);

    /**
     * Creates LogstashMarker with conversation id from the putMessageRequest that will appear
     * in the logs when used.
     *
     * @param requestAdapter request that contains journal post id, receiver party number, sender party number and conversation id
     * @return LogstashMarker
     */
    public static LogstashMarker markerFrom(PutMessageRequestWrapper requestAdapter) {
        final LogstashMarker messageTypeMarker = MarkerFactory.messageTypeMarker(requestAdapter.getMessageType().name().toLowerCase());
        final LogstashMarker receiverMarker = MarkerFactory.receiverMarker(requestAdapter.getRecieverPartyNumber());
        final LogstashMarker senderMarker = MarkerFactory.senderMarker(requestAdapter.getSenderPartynumber());
        final LogstashMarker conversationIdMarker = MarkerFactory.conversationIdMarker(requestAdapter.getConversationId());
        final LogstashMarker markers = conversationIdMarker.and(receiverMarker).and(senderMarker).and(messageTypeMarker);

        if(requestAdapter.hasPayload() && !isAppReceipt(requestAdapter.getPayload())) {
            try {
                return journalPostIdMarker(requestAdapter.getJournalPostId()).and(markers);
            } catch (PayloadException e) {
                logger.error(markers, "We don't want to end execution because of logging problems", e);
            }
        }
        return markers;
    }


    /**
     * Marker used to log payload size of the message.
     * @return marker
     */
    public static LogstashMarker payloadSizeMarker(byte[] bytes) {
        return Markers.append(PAYLOAD_SIZE, FileUtils.byteCountToDisplaySize(bytes.length));
    }


    public static LogstashMarker markerFrom(PutMessageResponseType response) {
        LogstashMarker marker = responseTypeMarker(response.getResult().getType());
        for (StatusMessageType s : response.getResult().getMessage()) {
            marker.and(responseMessageTextMarker(s.getText()));
            marker.and(responseMessageCodeMarker(s.getCode()));
        }
        return marker;
    }





    /**
     * Creates LogstashMarker with conversation id from the StandardBusinessDocument that will appear
     * in the logs when used.
     *
     * @param documentWrapper wrapper around StandardBusinessDocument
     * @return LogstashMarker
     */
    public static LogstashMarker markerFrom(StandardBusinessDocumentWrapper documentWrapper) {
        LogstashMarker messageTypeMarker = MarkerFactory.messageTypeMarker(documentWrapper.getMessageType());
        LogstashMarker journalPostIdMarker = journalPostIdMarker(documentWrapper.getJournalPostId());
        LogstashMarker documentIdMarker = Markers.append(DOCUMENT_ID, documentWrapper.getDocumentId());
        LogstashMarker conversationIdMarker = conversationIdMarker(documentWrapper.getConversationId());
        final LogstashMarker receiverMarker = receiverMarker(documentWrapper.getReceiverOrgNumber());
        final LogstashMarker senderMarker = senderMarker(documentWrapper.getSenderOrgNumber());
        return documentIdMarker.and(journalPostIdMarker).and(conversationIdMarker).and(senderMarker).and(receiverMarker).and(messageTypeMarker);
    }




    public static LogstashMarker markerFrom(FileReference reference) {
        LogstashMarker receiptIdMarker = Markers.append(ALTINN_RECEIPT_ID, reference.getReceiptID());
        LogstashMarker referenceValueMarker = Markers.append("altinn-reference-value", reference.getValue());
        return receiptIdMarker.and(referenceValueMarker);
    }


}
