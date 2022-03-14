package no.difi.meldingsutveksling.cucumber;

import no.difi.meldingsutveksling.api.MessagePersister;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class InMemoryMessagePersister implements MessagePersister {

    private final Map<String, Map<String, byte[]>> data = new HashMap<>();

    @Override
    public void write(String messageId, String filename, byte[] message) {
        Map<String, byte[]> files = getFiles(messageId);
        files.put(filename, Arrays.copyOf(message, message.length));
        data.put(messageId, files);
    }

    @Override
    public void writeStream(String messageId, String filename, InputStream stream, long size) throws IOException {
        Map<String, byte[]> files = getFiles(messageId);
        files.put(filename, IOUtils.toByteArray(stream));
        data.put(messageId, files);
    }

    @Override
    public byte[] read(String messageId, String filename) {
        Map<String, byte[]> files = getFiles(messageId);
        return files.get(filename);
    }

    @Override
    public FileEntryStream readStream(String messageId, String filename) {
        Map<String, byte[]> files = getFiles(messageId);
        byte[] bytes = files.get(filename);
        return FileEntryStream.of(new BufferedInputStream(new ByteArrayInputStream(bytes)), bytes.length);
    }

    @Override
    public void delete(String messageId) {
        data.remove(messageId);
    }

    private Map<String, byte[]> getFiles(String messageId) {
        return data.getOrDefault(messageId, new HashMap<>());
    }
}
