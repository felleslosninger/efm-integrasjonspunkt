package no.difi.meldingsutveksling.api;

import org.springframework.core.io.Resource;

import java.io.IOException;

public interface MessagePersister {

    void write(String messageId, String filename, Resource resource);

    Resource read(String messageId, String filename);

    void delete(String messageId) throws IOException;
}
