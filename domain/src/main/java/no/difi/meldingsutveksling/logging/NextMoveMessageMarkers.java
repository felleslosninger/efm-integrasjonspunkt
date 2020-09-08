package no.difi.meldingsutveksling.logging;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.move.common.IdentifierHasher;

public class NextMoveMessageMarkers {

    private NextMoveMessageMarkers() {
    }

    public static LogstashMarker markerFrom(NextMoveMessage message) {
        final LogstashMarker conversationIdMarker = MarkerFactory.conversationIdMarker(message.getConversationId());
        final LogstashMarker messageIdMarker = MarkerFactory.messageIdMarker(message.getMessageId());
        final LogstashMarker senderMarker = senderMarker(message.getSbd().getSender().asIso6523());
        final LogstashMarker senderIdentifierMarker = MarkerFactory.senderMarker(message.getSenderIdentifier());
        final LogstashMarker receiverMarker = receiverMarker(message.getSbd().getReceiver().asIso6523());
        final LogstashMarker receiverIdentifierMarker = MarkerFactory.receiverMarker(message.getReceiverIdentifier());
        final LogstashMarker messagetypeIdMarker = MarkerFactory.messageTypeMarker(message.getServiceIdentifier().toString());
        final LogstashMarker processMarker = processMarker(message.getSbd().getProcess());
        final LogstashMarker documentTypeMarker = documentTypeMarker(message.getSbd().getDocumentId());
        return conversationIdMarker.and(messageIdMarker)
                .and(senderMarker)
                .and(senderIdentifierMarker)
                .and(receiverMarker)
                .and(receiverIdentifierMarker)
                .and(messagetypeIdMarker)
                .and(processMarker)
                .and(documentTypeMarker);
    }

    public static LogstashMarker senderMarker(String sender) {
        return Markers.append("sender", sender);
    }

    public static LogstashMarker receiverMarker(String receiver) {
        return Markers.append("receiver", IdentifierHasher.hashIfPersonnr(receiver));
    }

    public static LogstashMarker processMarker(String processIdentifier) {
        return Markers.append("process_identifier", processIdentifier);
    }

    public static LogstashMarker documentTypeMarker(String documentIdentifier) {
        return Markers.append("documenttype_identifier", documentIdentifier);
    }

}
