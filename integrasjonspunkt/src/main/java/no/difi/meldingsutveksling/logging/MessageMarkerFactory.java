package no.difi.meldingsutveksling.logging;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.noarkexchange.JournalpostId;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestAdapter;

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
    public static final String JOURNALPOST_ID = "journalpost_id";

    /**
     * Creates LogstashMarker with conversation id from the putMessageRequest that will appear
     * in the logs when used.
     *
     * @param requestAdapter
     * @return LogstashMarker
     */
    public static LogstashMarker markerFrom(PutMessageRequestAdapter requestAdapter) {
        LogstashMarker journalPostIdMarker = Markers.append(JOURNALPOST_ID, JournalpostId.fromPutMessage(requestAdapter).value());
        return Markers.append(CONVERSATION_ID, requestAdapter.getConversationId()).and(journalPostIdMarker);
    }

}
