package no.difi.meldingsutveksling.nextmove;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.logging.MarkerFactory;

public class NextMoveMessageMarkers {

    private NextMoveMessageMarkers() {
    }

    public static LogstashMarker markerFrom(ConversationResource resource) {

        final LogstashMarker conversationIdMarker = MarkerFactory.conversationIdMarker(resource.getConversationId());
        final LogstashMarker senderIdMarker = MarkerFactory.senderMarker(resource.getSenderId());
        final LogstashMarker receiverMarker = MarkerFactory.receiverMarker(resource.getReceiverId());
        final LogstashMarker messagetypeIdMarker = MarkerFactory.messageTypeMarker(resource.getServiceIdentifier().toString());
        return conversationIdMarker.and(senderIdMarker).and(receiverMarker).and(messagetypeIdMarker);
    }

    public static LogstashMarker markerFrom(NextMoveMessage message) {

        final LogstashMarker conversationIdMarker = MarkerFactory.conversationIdMarker(message.getConversationId());
        final LogstashMarker senderIdMarker = MarkerFactory.senderMarker(message.getSenderIdentifier());
        final LogstashMarker receiverMarker = MarkerFactory.receiverMarker(message.getReceiverIdentifier());
        final LogstashMarker messagetypeIdMarker = MarkerFactory.messageTypeMarker(message.getServiceIdentifier().toString());
        return conversationIdMarker.and(senderIdMarker).and(receiverMarker).and(messagetypeIdMarker);
    }
}
