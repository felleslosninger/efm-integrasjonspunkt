package no.difi.meldingsutveksling.logging;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;

public class NextMoveMessageMarkers {

    private NextMoveMessageMarkers() {
    }

    public static LogstashMarker markerFrom(NextMoveMessage message) {
        final LogstashMarker conversationIdMarker = MarkerFactory.conversationIdMarker(message.getConversationId());
        final LogstashMarker senderIdMarker = MarkerFactory.senderMarker(message.getSenderIdentifier());
        final LogstashMarker receiverMarker = MarkerFactory.receiverMarker(message.getReceiverIdentifier());
        final LogstashMarker messagetypeIdMarker = MarkerFactory.messageTypeMarker(message.getServiceIdentifier().toString());
        final LogstashMarker processMarker = processMarker(message.getSbd().getProcess());
        final LogstashMarker documentTypeMarker = documentTypeMarker(message.getSbd().getDocumentId());
        return conversationIdMarker.and(senderIdMarker).and(receiverMarker).and(messagetypeIdMarker).and(processMarker).and(documentTypeMarker);
    }

    public static LogstashMarker processMarker(String processIdentifier) {
        return Markers.append("process-identifier", processIdentifier);
    }

    public static LogstashMarker documentTypeMarker(String documentIdentifier) {
        return Markers.append("documenttype-identifier", documentIdentifier);
    }

}
