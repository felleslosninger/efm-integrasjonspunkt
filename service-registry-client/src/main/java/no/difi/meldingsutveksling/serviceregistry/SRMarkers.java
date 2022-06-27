package no.difi.meldingsutveksling.serviceregistry;

import com.google.common.base.Strings;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.logging.MarkerFactory;
import no.difi.move.common.IdentifierHasher;

public class SRMarkers {

    public static LogstashMarker markerFrom(SRParameter srparam) {
        return Markers.append("identifier", Strings.isNullOrEmpty(srparam.getIdentifier().getIdentifier()) ? null : IdentifierHasher.hashIfPersonnr(srparam.getIdentifier().getIdentifier()))
                .and(MarkerFactory.conversationIdMarker(srparam.getConversationId()))
                .and(Markers.append("process_identifier", srparam.getProcess()));
    }

    public static LogstashMarker markerFrom(SRParameter srparam, String documentType) {
        return markerFrom(srparam)
                .and(Markers.append("documenttype_identifier", documentType));
    }

}
