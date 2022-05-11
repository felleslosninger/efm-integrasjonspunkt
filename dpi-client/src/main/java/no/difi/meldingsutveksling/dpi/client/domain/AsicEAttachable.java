package no.difi.meldingsutveksling.dpi.client.domain;

import org.springframework.core.io.Resource;

public interface AsicEAttachable {
    String getFilename();

    Resource getResource();

    String getMimeType();
}
