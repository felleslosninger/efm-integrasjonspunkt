package no.difi.meldingsutveksling.receipt;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.logging.MarkerFactory;

/**
 * Creates markers for Conversations.
 */
public class ConversationMarker {

    private static final String SERVICE_IDENTIFIER = "service_identifier";
    private static final String STATUS = "status";
    private static final String DESCRIPTION = "description";

    private ConversationMarker() {
    }

    public static LogstashMarker markerFrom(Conversation conversation) {
        LogstashMarker conversationIdMarker = MarkerFactory.conversationIdMarker(conversation.getConversationId());
        LogstashMarker senderMarker = MarkerFactory.senderMarker(conversation.getSenderIdentifier());
        LogstashMarker receiverMarker = MarkerFactory.receiverMarker(conversation.getReceiverIdentifier());
        LogstashMarker serviceIdentifierMarker = serviceIdentifierMarker(conversation.getServiceIdentifier());
        return conversationIdMarker.and(senderMarker).and(receiverMarker).and(serviceIdentifierMarker);
    }

    public static LogstashMarker markerFrom(MessageStatus status) {
        return statusMarker(status.getStatus()).and(descriptionMarker(status.getDescription()));
    }

    public static LogstashMarker serviceIdentifierMarker(ServiceIdentifier serviceIdentifier) {
        return Markers.append(SERVICE_IDENTIFIER, serviceIdentifier.toString());
    }

    public static LogstashMarker statusMarker(String status) {
        return Markers.append(STATUS, status);
    }

    public static LogstashMarker descriptionMarker(String description) {
        return Markers.append(DESCRIPTION, description);
    }
}
