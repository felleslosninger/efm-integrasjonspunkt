package no.difi.meldingsutveksling.receipt;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;

/**
 * Creates markers for Conversations.
 */
public class ConversationMarker {
    private static final String MESSAGE_ID = "message_id";

    public static LogstashMarker markerFrom(Conversation conversation) {
        final LogstashMarker messageIdMarker = Markers.append(MESSAGE_ID, conversation.getConversationId());
        return messageIdMarker;
    }

}
