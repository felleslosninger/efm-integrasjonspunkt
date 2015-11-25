package no.difi.meldingsutveksling.logging;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestAdapter;

/**
 * Example usage:
 * import static no.difi.meldingsutveksling.logging.ConversationIdMarkerFactory.markerFrom;
 *
 * ...
 *
 * log.error(markerFrom(message), "putting message");
 */
public class ConversationIdMarkerFactory {
    public static final String CONVERSATION_ID = "conversation_id";

    /**
     * Creates LogstashMarker with conversation id from the putMessageRequest that will appear
     * in the logs when used.
     *
     * @param requestAdapter
     * @return LogstashMarker
     */
    public static LogstashMarker markerFrom(PutMessageRequestAdapter requestAdapter) {
        return Markers.append(CONVERSATION_ID, requestAdapter.getConversationId());
    }

}
