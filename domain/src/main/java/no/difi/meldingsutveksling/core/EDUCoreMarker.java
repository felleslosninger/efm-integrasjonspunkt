package no.difi.meldingsutveksling.core;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.logging.MarkerFactory;

public class EDUCoreMarker {

    /**
     * Creates LogstashMarker with id from the request that will appear in the logs when used.
     *
     * @param message request that contains journal post id, receiver party number, sender party number and conversation id
     * @return LogstashMarker
     */
    public static LogstashMarker markerFrom(EDUCore message) {
        final LogstashMarker messageTypeMarker = MarkerFactory.messageTypeMarker(message.getMessageType().name().toLowerCase());
        final LogstashMarker receiverMarker = MarkerFactory.receiverMarker(message.getReceiver().getOrgNr());
        final LogstashMarker senderMarker = MarkerFactory.senderMarker(message.getSender().getOrgNr());
        final LogstashMarker idMarker = MarkerFactory.conversationIdMarker(message.getId());
        final LogstashMarker markers = idMarker.and(receiverMarker).and(senderMarker).and(messageTypeMarker);

        if(message.hasPayload() && (message.getMessageType() != EDUCore.MessageType.APPRECEIPT)) {
            return MarkerFactory.journalPostIdMarker(message.getPayloadAsMeldingType().getJournpost().getJpId()).and(markers);
        }
        return markers;
    }
}
