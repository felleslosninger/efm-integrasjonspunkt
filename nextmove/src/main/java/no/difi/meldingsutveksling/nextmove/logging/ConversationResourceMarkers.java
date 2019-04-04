package no.difi.meldingsutveksling.nextmove.logging;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.logging.MarkerFactory;
import no.difi.meldingsutveksling.nextmove.ConversationResource;

public class ConversationResourceMarkers {

    private ConversationResourceMarkers() {
    }

    public static LogstashMarker markerFrom(ConversationResource resource) {

        final LogstashMarker conversationIdMarker = MarkerFactory.conversationIdMarker(resource.getConversationId());
        final LogstashMarker senderIdMarker = MarkerFactory.senderMarker(resource.getSenderId());
        final LogstashMarker receiverMarker = MarkerFactory.receiverMarker(resource.getReceiverId());
        final LogstashMarker messagetypeIdMarker = MarkerFactory.messageTypeMarker(serviceIdentifierLoggingOverride(resource));
        return conversationIdMarker.and(senderIdMarker).and(receiverMarker).and(messagetypeIdMarker);
    }

    /**
     * Override for service identifier logging, in case of meeting message. Should be removed with the next version of NextMove.
     * @param cr ConversationResource
     * @return serviceIdentifier used for logging
     */
    public static String serviceIdentifierLoggingOverride(ConversationResource cr) {
        if (cr.getCustomProperties() != null &&
                cr.getCustomProperties().containsKey("meeting") &&
                cr.getCustomProperties().get("meeting").equals("true")) {
            return "DPE_MEETING";
        }
        return cr.getServiceIdentifier().toString();
    }
}
