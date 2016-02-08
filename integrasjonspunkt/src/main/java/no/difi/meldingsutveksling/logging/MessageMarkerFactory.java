package no.difi.meldingsutveksling.logging;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType;


/**
 * Example usage:
 * import static no.difi.meldingsutveksling.logging.ConversationIdMarkerFactory.markerFrom;
 * <p/>
 * ...
 * <p/>
 * log.error(markerFrom(message), "putting message");
 */
public class MessageMarkerFactory {
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String DOCUMENT_ID = "document_id";
    public static final String JOURNALPOST_ID = "journalpost_id";
    public static final String RECEIVER_ORG_NUMBER = "receiver_org_number";
    private static final String SENDER_ORG_NUMBER = "sender_org_number";
    public static final String ALTINN_RECEIPT_ID = "altinn-receipt-id";

    public static final String RESPONSE_TYPE = "response-type";
    public static final String RESPONSE_STATUS_MESSAGE_TEXT = "response-message-text";
    public static final String RESPONSE_STATUS_MESSAGE_CODE = "response-message-code";


    /**
     * Creates LogstashMarker with conversation id from the putMessageRequest that will appear
     * in the logs when used.
     *
     * @param requestWrapper request that contains journal post id, receiver party number, sender party number and conversation id
     * @return LogstashMarker
     */
    public static LogstashMarker markerFrom(PutMessageRequestWrapper requestAdapter) {
        LogstashMarker journalPostIdMarker = journalPostIdMarker(JournalpostId.fromPutMessage(requestAdapter).value());
        final LogstashMarker receiverMarker = receiverMarker(requestAdapter.getRecieverPartyNumber());
        final LogstashMarker senderMarker = senderMarker(requestAdapter.getSenderPartynumber());
        final LogstashMarker conversationIdMarker = conversationIdMarker(requestAdapter.getConversationId());
        return conversationIdMarker.and(journalPostIdMarker).and(receiverMarker).and(senderMarker);
    }

    public static LogstashMarker markerFrom(PutMessageResponseType response) {
        final LogstashMarker marker = responseTypeMarker(response.getResult().getType());
        for (StatusMessageType s : response.getResult().getMessage()) {
            marker.and(responseMessageTextMarker(s.getText()));
            marker.and(responseMessageCodeMarker(s.getCode()));
        }
        return marker;
    }


    private static LogstashMarker conversationIdMarker(String conversationId) {
        return Markers.append(CONVERSATION_ID, conversationId);
    }

    private static LogstashMarker journalPostIdMarker(String journalPostId) {
        return Markers.append(JOURNALPOST_ID, journalPostId);
    }

    private static LogstashMarker receiverMarker(String recieverPartyNumber) {
        return Markers.append(RECEIVER_ORG_NUMBER, recieverPartyNumber);
    }

    private static LogstashMarker senderMarker(String senderPartynumber) {
        return Markers.append(SENDER_ORG_NUMBER, senderPartynumber);
    }

    private static LogstashMarker responseTypeMarker(String senderPartynumber) {
        return Markers.append(RESPONSE_TYPE, senderPartynumber);
    }

    private static LogstashMarker responseMessageCodeMarker(String senderPartynumber) {
        return Markers.append(RESPONSE_STATUS_MESSAGE_CODE, senderPartynumber);
    }

    private static LogstashMarker responseMessageTextMarker(String senderPartynumber) {
        return Markers.append(RESPONSE_STATUS_MESSAGE_TEXT, senderPartynumber);
    }


    /**
     * Creates LogstashMarker with conversation id from the StandardBusinessDocument that will appear
     * in the logs when used.
     *
     * @param documentWrapper wrapper around StandardBusinessDocument
     * @return LogstashMarker
     */
    public static LogstashMarker markerFrom(StandardBusinessDocumentWrapper documentWrapper) {
        LogstashMarker journalPostIdMarker = journalPostIdMarker(documentWrapper.getJournalPostId());
        LogstashMarker documentIdMarker = Markers.append(DOCUMENT_ID, documentWrapper.getDocumentId());
        LogstashMarker conversationIdMarker = conversationIdMarker(documentWrapper.getConversationId());
        final LogstashMarker receiverMarker = receiverMarker(documentWrapper.getReceiverOrgNumber());
        final LogstashMarker senderMarker = senderMarker(documentWrapper.getSenderOrgNumber());
        return documentIdMarker.and(journalPostIdMarker).and(conversationIdMarker).and(senderMarker).and(receiverMarker);
    }


    public static LogstashMarker markerFrom(FileReference reference) {
        LogstashMarker receiptIdMarker = Markers.append(ALTINN_RECEIPT_ID, reference.getReceiptID());
        LogstashMarker referenceValueMarker = Markers.append("altinn-reference-value", reference.getValue());
        return receiptIdMarker.and(referenceValueMarker);
    }


}
