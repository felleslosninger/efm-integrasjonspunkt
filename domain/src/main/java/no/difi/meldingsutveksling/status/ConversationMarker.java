package no.difi.meldingsutveksling.status;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.logging.MarkerFactory;
import no.difi.meldingsutveksling.logging.NextMoveMessageMarkers;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;

/**
 * Creates markers for Conversations.
 */
public class ConversationMarker {

    private static final String SERVICE_IDENTIFIER = "service_identifier";
    private static final String STATUS = "status";
    private static final String DESCRIPTION = "description";
    private static final String DIRECTION = "direction";
    private static final String PROCESS = "process_identifier";
    private static final String DOCUMENT = "document_identifier";

    private ConversationMarker() {
    }

    public static LogstashMarker markerFrom(Conversation conversation) {
        LogstashMarker conversationIdMarker = MarkerFactory.conversationIdMarker(conversation.getConversationId());
        LogstashMarker messageIdMarker = MarkerFactory.messageIdMarker(conversation.getMessageId());
        LogstashMarker senderMarker = NextMoveMessageMarkers.senderMarker(conversation.getSender());
        LogstashMarker senderIdentifierMarker = MarkerFactory.senderMarker(conversation.getSenderIdentifier());
        LogstashMarker receiverMarker = NextMoveMessageMarkers.receiverMarker(conversation.getReceiver());
        LogstashMarker receiverIdentifierMarker = NextMoveMessageMarkers.receiverOrgnrMarker(conversation.getReceiverIdentifier());
        LogstashMarker processMarker = processIdentifierMarker(conversation.getProcessIdentifier());
        LogstashMarker serviceIdentifierMarker = serviceIdentifierMarker(conversation.getServiceIdentifier());
        LogstashMarker directionMarker = directionMarker(conversation.getDirection());
        LogstashMarker documentTypeMarker = documentIdentifierMarker(conversation.getDocumentIdentifier());
        return conversationIdMarker.and(messageIdMarker)
            .and(senderMarker)
            .and(senderIdentifierMarker)
            .and(receiverMarker)
            .and(receiverIdentifierMarker)
            .and(processMarker)
            .and(documentTypeMarker)
            .and(serviceIdentifierMarker)
            .and(directionMarker);
    }

    public static LogstashMarker markerFrom(MessageStatus status) {
        return statusMarker(status.getStatus()).and(descriptionMarker(status.getDescription()));
    }

    private static LogstashMarker serviceIdentifierMarker(ServiceIdentifier serviceIdentifier) {
        return Markers.append(SERVICE_IDENTIFIER, serviceIdentifier.toString());
    }

    private static LogstashMarker processIdentifierMarker(String processIdentifier) {
        return Markers.append(PROCESS, processIdentifier);
    }

    public static LogstashMarker documentIdentifierMarker(String documentIdentifier) {
        return Markers.append(DOCUMENT, documentIdentifier);
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
