package no.difi.meldingsutveksling.receipt;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.logging.MarkerFactory;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;

/**
 * Creates markers for Conversations.
 */
public class ConversationMarker {

    private static final String SERVICE_IDENTIFIER = "service_identifier";
    private static final String STATUS = "status";
    private static final String DESCRIPTION = "description";
    private static final String DIRECTION = "direction";

    private ConversationMarker() {
    }

    public static LogstashMarker markerFrom(Conversation conversation) {
        LogstashMarker conversationIdMarker = MarkerFactory.conversationIdMarker(conversation.getConversationId());
        LogstashMarker messageIdMarker = MarkerFactory.messageIdMarker(conversation.getMessageId());
        LogstashMarker senderMarker = MarkerFactory.senderMarker(conversation.getSenderIdentifier());
        LogstashMarker receiverMarker = MarkerFactory.receiverMarker(conversation.getReceiverIdentifier());
        LogstashMarker serviceIdentifierMarker = serviceIdentifierMarker(conversation.getServiceIdentifier());
        LogstashMarker directionMarker = directionMarker(conversation.getDirection());
        return conversationIdMarker.and(messageIdMarker).and(senderMarker).and(receiverMarker).and(serviceIdentifierMarker).and(directionMarker);
    }

    public static LogstashMarker markerFrom(MessageStatus status) {
        return statusMarker(status.getStatus()).and(descriptionMarker(status.getDescription()));
    }

    private static LogstashMarker serviceIdentifierMarker(ServiceIdentifier serviceIdentifier) {
        return Markers.append(SERVICE_IDENTIFIER, serviceIdentifier.toString());
    }

    private static LogstashMarker statusMarker(String status) {
        return Markers.append(STATUS, status);
    }

    private static LogstashMarker descriptionMarker(String description) {
        return Markers.append(DESCRIPTION, description);
    }

    private static LogstashMarker directionMarker(ConversationDirection direction) {
        return Markers.append(DIRECTION, direction.toString());
    }
}
