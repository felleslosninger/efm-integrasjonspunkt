package no.difi.meldingsutveksling.api;

import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.pipes.Reject;

import java.io.IOException;
import java.io.InputStream;

public interface OptionalCryptoMessagePersister {

    void write(String messageId, String filename, byte[] message) throws IOException;

    void writeStream(String messageId, String filename, InputStream stream) throws IOException;

    byte[] read(String messageId, String filename) throws IOException;

    FileEntryStream readStream(String messageId, String filename, Reject reject);

    void delete(String messageId) throws IOException;
}
