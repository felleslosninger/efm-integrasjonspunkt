package no.difi.meldingsutveksling.dokumentpakking.service;

import lombok.SneakyThrows;
import no.difi.asic.AsicReader;
import no.difi.asic.AsicReaderFactory;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class AsicParser {

    public List<Document> parse(Resource asic) {
        return parse(asic, this::getByteArrayResource);
    }

    @SneakyThrows
    private ByteArrayResource getByteArrayResource(InputStream attachmentStream) {
        return new ByteArrayResource(StreamUtils.copyToByteArray(attachmentStream));
    }

    @SneakyThrows
    public List<Document> parse(Resource asic, Function<InputStream, Resource> resourceFactory) {
        final List<Document> attachments = new ArrayList<>();

        parse(asic, (filename, inputStream) -> attachments.add(Document.builder()
                .resource(resourceFactory.apply(inputStream))
                .filename(filename)
                .mimeType(MediaType.APPLICATION_OCTET_STREAM)
                .build()
        ));

        return attachments;
    }

    @SneakyThrows
    public void parse(Resource asic, Listener listener) {
        try (InputStream inputStream = asic.getInputStream()) {
            try (AsicReader asicReader = AsicReaderFactory.newFactory().open(inputStream)) {

                String filename;

                while ((filename = asicReader.getNextFile()) != null) {
                    try (InputStream is = asicReader.inputStream()) {
                        listener.onFile(filename, is);
                    }
                }
            }
        }
    }

    public interface Listener {

        void onFile(String filename, InputStream inputStream);
    }
}