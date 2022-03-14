package no.difi.meldingsutveksling.api;

import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import java.io.IOException;

public interface MessagePersister {

    void write(String messageId, String filename, Resource resource) throws IOException;

    byte[] readBytes(String messageId, String filename) throws IOException;

    Resource read(String messageId, String filename) throws IOException;

    void read(String messageId, String filename, WritableResource writableResource) throws IOException;

    void delete(String messageId) throws IOException;
}
