package no.difi.meldingsutveksling.mxa;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageMarker {
    public static final Logger logger = LoggerFactory.getLogger(MessageMarker.class);

    private static final String PARTICIPANT_ID = "participant_id";
    private static final String MESSAGE_REF = "message_reference";
    private static final String IDPROC = "idproc";

    public static LogstashMarker markerFrom(Message msg) {
        final LogstashMarker participantMarker = participantIdMarker(msg.getParticipantId());
        final LogstashMarker messageRefMarker = messageRefMarker(msg.getMessageReference());
        final LogstashMarker idprocMarker = idprocMarker(msg.getIdproc());
        final LogstashMarker markers = participantMarker.and(messageRefMarker).and(idprocMarker);

        return markers;
    }

    private static LogstashMarker participantIdMarker(String participantId) {
        return Markers.append(PARTICIPANT_ID, participantId);
    }

    private static LogstashMarker messageRefMarker(String messageRef) {
        return Markers.append(MESSAGE_REF, messageRef);
    }

    private static LogstashMarker idprocMarker(String idproc) {
        return Markers.append(IDPROC, idproc);
    }
}
