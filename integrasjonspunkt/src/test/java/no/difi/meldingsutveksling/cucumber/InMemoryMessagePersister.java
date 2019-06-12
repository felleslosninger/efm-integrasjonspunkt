package no.difi.meldingsutveksling.cucumber;

import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
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
    public void write(String conversationId, String filename, byte[] message) {
        Map<String, byte[]> files = getFiles(conversationId);
        files.put(filename, Arrays.copyOf(message, message.length));
        data.put(conversationId, files);
    }

    @Override
    public void writeStream(String conversationId, String filename, InputStream stream, long size) throws IOException {
        Map<String, byte[]> files = getFiles(conversationId);
        files.put(filename, IOUtils.toByteArray(stream));
        data.put(conversationId, files);
    }

    @Override
    public byte[] read(String conversationId, String filename) {
        Map<String, byte[]> files = getFiles(conversationId);
        return files.get(filename);
    }

    @Override
    public FileEntryStream readStream(String conversationId, String filename) {
        Map<String, byte[]> files = getFiles(conversationId);
        byte[] bytes = files.get(filename);
        return FileEntryStream.of(new BufferedInputStream(new ByteArrayInputStream(bytes)), bytes.length);
    }

    @Override
    public void delete(String conversationId) {
        data.remove(conversationId);
    }

    private Map<String, byte[]> getFiles(String conversationId) {
        return data.getOrDefault(conversationId, new HashMap<>());
    }
}
