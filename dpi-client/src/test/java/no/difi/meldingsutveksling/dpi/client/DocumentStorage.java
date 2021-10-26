package no.difi.meldingsutveksling.dpi.client;

import org.springframework.core.io.Resource;

import java.io.InputStream;

public interface DocumentStorage {

    Resource read(String messageId, String filename);

    void write(String messageId, String filename, InputStream resource);
}
