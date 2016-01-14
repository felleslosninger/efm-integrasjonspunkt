package no.difi.meldingsutveksling.logging;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.noarkexchange.JournalpostId;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentWrapper;

/**
 * Example usage:
 * import static no.difi.meldingsutveksling.logging.ConversationIdMarkerFactory.markerFrom;
 *
 * ...
 *
 * log.error(markerFrom(message), "putting message");
 */
public class MessageMarkerFactory {
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String DOCUMENT_ID = "document_id";
    public static final String JOURNALPOST_ID = "journalpost_id";
    public static final String RECEIVER_ORG_NUMBER = "receiver_org_number";

    /**
     * Creates LogstashMarker with conversation id from the putMessageRequest that will appear
     * in the logs when used.
     *
     * @param requestAdapter
     * @return LogstashMarker
     */
    public static LogstashMarker markerFrom(PutMessageRequestWrapper requestAdapter) {
        LogstashMarker journalPostIdMarker = Markers.append(JOURNALPOST_ID, JournalpostId.fromPutMessage(requestAdapter).value());
        final LogstashMarker receiverMarker = Markers.append(RECEIVER_ORG_NUMBER, requestAdapter.hasRecieverPartyNumber());
        return Markers.append(CONVERSATION_ID, requestAdapter.getConversationId()).and(journalPostIdMarker).and(receiverMarker);
    }

    /**
     * Creates LogstashMarker with conversation id from the StandardBusinessDocument that will appear
     * in the logs when used.
     *
     * @param documentWrapper wrapper around StandardBusinessDocument
     * @return LogstashMarker
     */
    public static LogstashMarker markerFrom(StandardBusinessDocumentWrapper documentWrapper) {
        LogstashMarker journalPostIdMarker = Markers.append(JOURNALPOST_ID, documentWrapper.getJournalPostId());
        LogstashMarker documentIdMarker = Markers.append(DOCUMENT_ID, documentWrapper.getDocumentId());
        LogstashMarker conversationIdMarker = Markers.append(CONVERSATION_ID, documentWrapper.getConversationId());
        Markers.append(RECEIVER_ORG_NUMBER, documentWrapper.getReceiverOrgNumber());
        return documentIdMarker.and(journalPostIdMarker).and(conversationIdMarker);
    }

}
