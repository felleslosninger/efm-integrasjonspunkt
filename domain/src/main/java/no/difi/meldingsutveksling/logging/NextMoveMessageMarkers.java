package no.difi.meldingsutveksling.logging;

import com.google.common.base.Strings;
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
        final LogstashMarker senderMarker = senderMarker(message.getSender().getIdentifier());
        final LogstashMarker senderIdentifierMarker = MarkerFactory.senderMarker(message.getSenderIdentifier());
        final LogstashMarker receiverMarker = receiverMarker(message.getReceiver().getIdentifier());
        final LogstashMarker receiverIdentifierMarker = MarkerFactory.receiverMarker(message.getReceiverIdentifier());
        final LogstashMarker messagetypeIdMarker = MarkerFactory.messageTypeMarker(message.getServiceIdentifier().toString());
        final LogstashMarker processMarker = processMarker(message.getSbd().getProcess());
        final LogstashMarker documentTypeMarker = documentTypeMarker(message.getSbd().getDocumentType());
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
        return Markers.append("receiver", Strings.isNullOrEmpty(receiver) ? receiver : IdentifierHasher.hashIfPersonnr(receiver));
    }

    public static LogstashMarker receiverOrgnrMarker(String receiverOrgnr) {
        return Markers.append("receiver_org_number", Strings.isNullOrEmpty(receiverOrgnr) ? receiverOrgnr : IdentifierHasher.hashIfPersonnr(receiverOrgnr));
    }

    public static LogstashMarker processMarker(String processIdentifier) {
        return Markers.append("process_identifier", processIdentifier);
    }

    public static LogstashMarker documentTypeMarker(String documentIdentifier) {
        return Markers.append("documenttype_identifier", documentIdentifier);
    }

}
