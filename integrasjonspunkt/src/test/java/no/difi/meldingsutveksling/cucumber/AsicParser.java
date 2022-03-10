package no.difi.meldingsutveksling.cucumber;

import lombok.SneakyThrows;
import no.difi.asic.AsicReader;
import no.difi.asic.AsicReaderFactory;
import no.difi.commons.asic.jaxb.asic.AsicFile;
import no.difi.commons.asic.jaxb.asic.AsicManifest;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AsicParser {

    @SneakyThrows
    List<Document> parse(Resource asic) {
        try (InputStream inputStream = asic.getInputStream()) {
            List<Document> attachments = new ArrayList<>();

            try (AsicReader asicReader = AsicReaderFactory.newFactory().open(inputStream)) {

                String filename;

                while ((filename = asicReader.getNextFile()) != null) {
                    attachments.add(Document.builder()
                            .resource(new ByteArrayResource(StreamUtils.copyToByteArray(asicReader.inputStream())))
                            .filename(filename)
                            .build()
                    );
                }

                AsicManifest asicManifest = asicReader.getAsicManifest();
                attachments.forEach(p -> p.withMimeType(getMimeType(asicManifest, p.getFilename())));
            }

            return attachments;
        }
    }

    private MimeType getMimeType(AsicManifest asicManifest, String filename) {
        return MimeType.valueOf(getFile(asicManifest, filename).getMimetype());
    }

    private AsicFile getFile(AsicManifest asicManifest, String filename) {
        return asicManifest.getFile().stream()
                .filter(p -> p.getName().equals(filename))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("File not found: %s", filename)));
    }
}
