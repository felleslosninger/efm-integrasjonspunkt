package no.difi.meldingsutveksling.nextmove.message;

import java.io.IOException;
import java.io.InputStream;

public interface CryptoMessagePersister {

    void write(String conversationId, String filename, byte[] message) throws IOException;

    void writeStream(String conversationId, String filename, InputStream stream, long size) throws IOException;


    byte[] read(String conversationId, String filename) throws IOException;

    FileEntryStream readStream(String conversationId, String filename) throws IOException;

    void delete(String conversationId) throws IOException;
}
