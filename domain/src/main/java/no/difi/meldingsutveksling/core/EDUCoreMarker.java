package no.difi.meldingsutveksling.core;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.logging.MarkerFactory;
import no.difi.meldingsutveksling.noarkexchange.JournalpostId;
import no.difi.meldingsutveksling.noarkexchange.PayloadException;

@Slf4j
public class EDUCoreMarker {

    /**
     * Creates LogstashMarker with id from the request that will appear in the logs when used.
     *
     * @param message request that contains journal post id, receiver party number, sender party number and conversation id
     * @return LogstashMarker
     */
    public static LogstashMarker markerFrom(EDUCore message) {
        final LogstashMarker messageTypeMarker = MarkerFactory.messageTypeMarker(message.getMessageType().name().toLowerCase());
        final LogstashMarker receiverMarker = MarkerFactory.receiverMarker(message.getReceiver().getIdentifier());
        final LogstashMarker senderMarker = MarkerFactory.senderMarker(message.getSender().getIdentifier());
        final LogstashMarker idMarker = MarkerFactory.conversationIdMarker(message.getId());
        final LogstashMarker markers = idMarker.and(receiverMarker).and(senderMarker).and(messageTypeMarker);

        if(message.getPayload() != null && (message.getMessageType() != EDUCore.MessageType.APPRECEIPT)) {
            try {
                return MarkerFactory.journalPostIdMarker(JournalpostId.fromPayload(message.getPayload()).value()).and(markers);
            } catch (PayloadException e) {
                log.error(markers, "We don't want to end execution because of logging problems", e);
            }
        }
        return markers;
    }
}
