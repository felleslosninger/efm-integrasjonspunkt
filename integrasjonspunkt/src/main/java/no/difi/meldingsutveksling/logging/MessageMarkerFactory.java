package no.difi.meldingsutveksling.logging;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType;
import org.apache.commons.io.FileUtils;

import static no.difi.meldingsutveksling.logging.MarkerFactory.*;


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

    private MessageMarkerFactory() { }



    /**
     * Marker used to log payload size of the message.
     * @param bytes of the payload
     * @return marker containing the size of the payload
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
