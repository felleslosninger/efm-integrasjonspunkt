package no.difi.meldingsutveksling.dpi.client;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class InMemoryDocumentStorage implements DocumentStorage {

    private final Map<String, Map<String, byte[]>> data = new HashMap<>();

    @Override
    public Resource read(String messageId, String filename) {
        Map<String, byte[]> files = getFiles(messageId);
        byte[] bytes = files.get(filename);
        return new ByteArrayResource(bytes);
    }

    @Override
    public void write(String messageId, String filename, InputStream inputStream) {
        Map<String, byte[]> files = getFiles(messageId);
        files.put(filename, toByteArray(inputStream));
        data.put(messageId, files);
    }

    private byte[] toByteArray(InputStream inputStream) {
        try {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read all bytes", e);
        }
    }

    private Map<String, byte[]> getFiles(String messageId) {
        return data.getOrDefault(messageId, new HashMap<>());
    }

}
