package no.difi.meldingsutveksling.receipt;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.logging.MarkerFactory;

/**
 * Creates markers for Conversations.
 */
public class ConversationMarker {

    private ConversationMarker() {
    }

    public static LogstashMarker markerFrom(Conversation conversation) {
        LogstashMarker conversationIdMarker = MarkerFactory.conversationIdMarker(conversation.getConversationId());
        LogstashMarker receiverMarker = MarkerFactory.receiverMarker(conversation.getReceiverIdentifier());
        return conversationIdMarker.and(receiverMarker);
    }

}
