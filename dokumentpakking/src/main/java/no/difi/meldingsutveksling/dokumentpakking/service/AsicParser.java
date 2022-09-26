package no.difi.meldingsutveksling.dokumentpakking.service;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Value;
import no.difi.asic.AsicReader;
import no.difi.asic.AsicReaderFactory;
import no.difi.commons.asic.jaxb.asic.AsicFile;
import no.difi.commons.asic.jaxb.asic.AsicManifest;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        OurListener listener = new OurListener(resourceFactory);
        parse(asic, listener);
        return listener.getDocuments();
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

                listener.onManifest(asicReader.getAsicManifest());
            }
        }
    }

    public interface Listener {

        void onFile(String filename, InputStream inputStream);

        default void onManifest(AsicManifest asicManifest) {
        }
    }

    private static class OurListener implements Listener {

        private final List<InternalDocument> internalDocuments = new ArrayList<>();
        @Getter private List<Document> documents;
        private final Function<InputStream, Resource> resourceFactory;

        private OurListener(Function<InputStream, Resource> resourceFactory) {
            this.resourceFactory = resourceFactory;
        }

        @Override
        public void onFile(String filename, InputStream inputStream) {
            internalDocuments.add(new InternalDocument(filename, resourceFactory.apply(inputStream)));
        }

        @Override
        public void onManifest(AsicManifest asicManifest) {
            Map<String, AsicFile> fileMap = asicManifest.getFile().stream().collect(Collectors.toMap(AsicFile::getName, Function.identity()));
            documents = internalDocuments.stream().map(p ->
                            Document.builder()
                                    .filename(p.getFilename())
                                    .resource(p.getResource())
                                    .mimeType(fileMap.get(p.getFilename()).getMimetype())
                                    .build())
                    .collect(Collectors.toList());
        }
    }

    @Value
    private static class InternalDocument {
        String filename;
        Resource resource;
    }
}