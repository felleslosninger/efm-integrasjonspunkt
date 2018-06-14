package no.difi.meldingsutveksling.nextmove.message;

import java.io.IOException;

public interface MessagePersister {

    void write(String conversationId, String filename, byte[] message) throws IOException;

    byte[] read(String conversationId, String filename) throws IOException;

    void delete(String conversationId) throws IOException;
}
