package no.difi.meldingsutveksling.shipping;

import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import org.slf4j.Marker;

public interface UploadRequest {
    String getSender();
    String getReceiver();
    String getSenderReference();

    EduDocument getPayload();

    /**
     * Used to get Markers needed to uniquely identify an upload when logging. In particular Audit
     * logging.
     *
     * @return nested Logstash markers to used with logging
     */
    Marker getMarkers();
}
