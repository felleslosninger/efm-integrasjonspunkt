package no.difi.meldingsutveksling.cucumber;

import lombok.SneakyThrows;
import no.difi.asic.AsicReader;
import no.difi.asic.AsicReaderFactory;
import no.difi.commons.asic.jaxb.asic.AsicFile;
import no.difi.commons.asic.jaxb.asic.AsicManifest;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AsicParser {

    @SneakyThrows
    List<Attachment> parse(InputStream is) {
        List<Attachment> attachments = new ArrayList<>();

        try (AsicReader asicReader = AsicReaderFactory.newFactory().open(is)) {

            String filename;

            while ((filename = asicReader.getNextFile()) != null) {
                attachments.add(new Attachment(IOUtils.toByteArray(asicReader.inputStream()))
                        .setFileName(filename)
                );
            }

            AsicManifest asicManifest = asicReader.getAsicManifest();
            attachments.forEach(p -> p.setMimeType(getMimeType(asicManifest, p.getFileName())));
        }

        return attachments;
    }

    private String getMimeType(AsicManifest asicManifest, String filename) {
        return getFile(asicManifest, filename).getMimetype();
    }

    private AsicFile getFile(AsicManifest asicManifest, String filename) {
        return asicManifest.getFile().stream()
                .filter(p -> p.getName().equals(filename))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("File not found: %s", filename)));
    }
}
