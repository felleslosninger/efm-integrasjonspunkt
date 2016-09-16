package no.difi.meldingsutveksling.mxa;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.logging.MarkerFactory;
import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageMarker {
    public static final Logger logger = LoggerFactory.getLogger(MessageMarker.class);

    public static LogstashMarker markerFrom(Message msg) {

        final LogstashMarker senderMarker = MarkerFactory.receiverMarker(msg.getParticipantId());
        final LogstashMarker messageRefMarker = MarkerFactory.journalPostIdMarker(msg.getMessageReference());
        final LogstashMarker markers = senderMarker.and(messageRefMarker);

        return markers;
    }
}
