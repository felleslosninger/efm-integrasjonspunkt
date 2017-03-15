package no.difi.meldingsutveksling.nextbest.logging;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.nextbest.ConversationResource;

public class ConversationResourceMarkers {

    private static final String CONVERSATION_ID = "conversation_id";
    private static final String SENDER_ID = "sender_id";
    private static final String RECEIVER_ID = "receiver_id";
    private static final String MESSAGETYPE_ID = "messagetype_id";

    public static LogstashMarker markerFrom(ConversationResource resource) {

        final LogstashMarker conversationIdMarker = Markers.append(CONVERSATION_ID, resource.getConversationId());
        final LogstashMarker senderIdMarker = Markers.append(SENDER_ID, resource.getSenderId());
        final LogstashMarker receiverMarker = Markers.append(RECEIVER_ID, resource.getReceiverId());
        final LogstashMarker messagetypeIdMarker = Markers.append(MESSAGETYPE_ID, resource.getMessagetypeId());
        return conversationIdMarker.and(senderIdMarker).and(receiverMarker).and(messagetypeIdMarker);
    }
}
