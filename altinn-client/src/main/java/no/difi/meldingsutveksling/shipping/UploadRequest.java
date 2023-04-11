package no.difi.meldingsutveksling.shipping;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.slf4j.Marker;
import org.springframework.core.io.Resource;

public interface UploadRequest {
    String getSender();

    String getReceiver();

    String getSenderReference();

    StandardBusinessDocument getPayload();

    Resource getAsic();

    /**
     * Used to get Markers needed to uniquely identify an upload when logging. In particular Audit
     * logging.
     *
     * @return nested Logstash markers to used with logging
     */
    Marker getMarkers();
}
