package no.difi.meldingsutveksling.nextmove;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;

public interface CorrespondenceAgencyClientProvider {

    CorrespondenceAgencyClient getClient(LogstashMarker logstashMarker, CorrespondenceAgencyConfiguration config);
}
