package no.difi.meldingsutveksling.nextbest.logging;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.logging.MarkerFactory;
import no.difi.meldingsutveksling.nextbest.ConversationResource;

public class ConversationResourceMarkers {

    private ConversationResourceMarkers() {
    }

    public static LogstashMarker markerFrom(ConversationResource resource) {

        final LogstashMarker conversationIdMarker = MarkerFactory.conversationIdMarker(resource.getConversationId());
        final LogstashMarker senderIdMarker = MarkerFactory.senderMarker(resource.getSenderId());
        final LogstashMarker receiverMarker = MarkerFactory.receiverMarker(resource.getReceiverId());
        final LogstashMarker messagetypeIdMarker = MarkerFactory.messageTypeMarker(resource.getMessagetypeId());
        return conversationIdMarker.and(senderIdMarker).and(receiverMarker).and(messagetypeIdMarker);
    }
}
