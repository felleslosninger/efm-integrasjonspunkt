package no.difi.meldingsutveksling.nextmove.message;

import java.io.IOException;
import java.io.InputStream;

public interface MessagePersister {

    void write(String messageId, String filename, byte[] message) throws IOException;

    void writeStream(String messageId, String filename, InputStream stream, long size) throws IOException;


    byte[] read(String messageId, String filename) throws IOException;

    FileEntryStream readStream(String messageId, String filename);

    void delete(String messageId) throws IOException;
}
