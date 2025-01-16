package no.difi.meldingsutveksling.api;

import org.springframework.core.io.Resource;

import java.io.IOException;

public interface OptionalCryptoMessagePersister {

    void write(String messageId, String filename, Resource resource) throws IOException;

    Resource read(String messageId, String filename) throws IOException;

    void delete(String messageId) throws IOException;

}
