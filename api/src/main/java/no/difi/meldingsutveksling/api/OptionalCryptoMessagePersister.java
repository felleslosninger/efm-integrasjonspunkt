package no.difi.meldingsutveksling.api;

import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

public interface OptionalCryptoMessagePersister {

    void write(String messageId, String filename, Resource resource);

    InMemoryWithTempFileFallbackResource read(String messageId, String filename);

    InputStreamResource read(String messageId, String filename, Reject reject);

    void delete(String messageId) throws IOException;
}
