package no.difi.meldingsutveksling.cucumber;

import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.move.common.io.ResourceUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InMemoryMessagePersister implements MessagePersister {

    private final Map<String, Map<String, byte[]>> data = new HashMap<>();

    @Override
    public void write(String messageId, String filename, Resource resource) throws IOException {
        Map<String, byte[]> files = getFiles(messageId);
        files.put(filename, ResourceUtils.toByteArray(resource));
        data.put(messageId, files);
    }

    @Override
    public Resource read(String messageId, String filename) {
        Map<String, byte[]> files = getFiles(messageId);
        return new ByteArrayResource(files.get(filename));
    }

    @Override
    public void delete(String messageId) {
        data.remove(messageId);
    }

    private Map<String, byte[]> getFiles(String messageId) {
        return data.getOrDefault(messageId, new HashMap<>());
    }
}
