package no.difi.meldingsutveksling.noarkexchange.logging;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType;

import static no.difi.meldingsutveksling.logging.MarkerFactory.responseMessageCodeMarker;
import static no.difi.meldingsutveksling.logging.MarkerFactory.responseMessageTextMarker;
import static no.difi.meldingsutveksling.logging.MarkerFactory.responseTypeMarker;

public class PutMessageResponseMarkers {

    public static LogstashMarker markerFrom(PutMessageResponseType response) {
        LogstashMarker marker = responseTypeMarker(response.getResult().getType());
        for (StatusMessageType s : response.getResult().getMessage()) {
            marker.and(responseMessageTextMarker(s.getText()));
            marker.and(responseMessageCodeMarker(s.getCode()));
        }
        return marker;
    }
}
