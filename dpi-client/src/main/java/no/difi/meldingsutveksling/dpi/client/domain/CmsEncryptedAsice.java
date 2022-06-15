package no.difi.meldingsutveksling.dpi.client.domain;

import lombok.Value;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource;

@Value
public class CmsEncryptedAsice implements AutoCloseable {

    InMemoryWithTempFileFallbackResource resource;

    @Override
    public void close() throws Exception {
        resource.close();
    }
}
